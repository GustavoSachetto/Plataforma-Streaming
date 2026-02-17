"use client";

import { useState, useRef } from "react";

import { FFmpeg } from "@ffmpeg/ffmpeg";
import { fetchFile, toBlobURL } from "@ffmpeg/util";

export default function FileUploader() {
    const [loaded, setLoaded] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [file, setFile] = useState<File | null>(null);
    const [progress, setProgress] = useState(0);
    const [status, setStatus] = useState("");
    const [uploadId, setUploadId] = useState("");
    const ffmpegRef = useRef<FFmpeg | null>(null);
    const messageRef = useRef<HTMLParagraphElement | null>(null);

    const load = async () => {
        setIsLoading(true);
        const baseURL = "https://unpkg.com/@ffmpeg/core@0.12.6/dist/umd";

        if (!ffmpegRef.current) {
            ffmpegRef.current = new FFmpeg();
        }
        const ffmpeg = ffmpegRef.current;

        ffmpeg.on("log", ({ message }) => {
            if (messageRef.current) messageRef.current.innerHTML = message;
            console.log(message);
        });

        try {
            await ffmpeg.load({
                coreURL: await toBlobURL(`${baseURL}/ffmpeg-core.js`, "text/javascript"),
                wasmURL: await toBlobURL(`${baseURL}/ffmpeg-core.wasm`, "application/wasm"),
            });
            setLoaded(true);
            setStatus("FFmpeg loaded");
        } catch (error) {
            console.error("Failed to load FFmpeg", error);
            setStatus("Failed to load FFmpeg");
        } finally {
            setIsLoading(false);
        }
    };

    const calculateSHA256 = async (blob: Blob): Promise<string> => {
        const buffer = await blob.arrayBuffer();
        const hashBuffer = await crypto.subtle.digest("SHA-256", buffer);
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        return hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");
    };

    const uploadFile = async () => {
        if (!file || !loaded) return;
        const ffmpeg = ffmpegRef.current;
        if (!ffmpeg) return;

        setStatus("Segmenting file...");
        const inputName = "input.mp4";
        await ffmpeg.writeFile(inputName, await fetchFile(file));

        await ffmpeg.exec([
            "-i", inputName,
            "-c", "copy",
            "-f", "segment",
            "-segment_time", "60",
            "-movflags", "+frag_keyframe+empty_moov+default_base_moof",
            "chunk%03d.mp4"
        ]);

        const files = await ffmpeg.listDir(".");
        const chunkFiles = files.filter((f) => f.name.startsWith("chunk") && f.name.endsWith(".mp4"));
        chunkFiles.sort((a, b) => a.name.localeCompare(b.name));

        if (chunkFiles.length === 0) {
            setStatus("No chunks created. Upload failed.");
            return;
        }

        setStatus(`Found ${chunkFiles.length} chunks. Calculating total hash...`);

        // Read all chunks to calculate total hash
        const chunksData: Uint8Array[] = [];
        let totalSize = 0;

        for (const chunkFile of chunkFiles) {
            const data = await ffmpeg.readFile(chunkFile.name);
            const uint8Data = typeof data === 'string'
                ? new TextEncoder().encode(data)
                : new Uint8Array(data as Uint8Array);
            chunksData.push(uint8Data);
            totalSize += uint8Data.length;
        }

        // Concatenate all chunks
        const fullBuffer = new Uint8Array(totalSize);
        let offset = 0;
        for (const chunkData of chunksData) {
            fullBuffer.set(chunkData, offset);
            offset += chunkData.length;
        }

        const fileHash = await calculateSHA256(new Blob([fullBuffer]));
        const filename = file.name;

        setStatus("Initializing upload...");

        const initRes = await fetch("http://localhost:8080/api/v1/upload/init", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                fileSize: totalSize,
                filename,
                fileHash
            })
        });

        if (!initRes.ok) {
            setStatus("Failed to initialize upload");
            // Cleanup memory if init fails
            chunksData.length = 0;
            return;
        }

        const { uploadId } = await initRes.json();
        setUploadId(uploadId);

        setStatus("Starting upload...");

        for (let i = 0; i < chunkFiles.length; i++) {
            const chunkName = chunkFiles[i].name;
            // Create a copy to ensure we use standard ArrayBuffer if needed, or cast to any to satisfy TS
            // (since standard Blob supports ArrayBufferView even if backed by SAB in some contexts, 
            // but strict TS types might complain).
            const chunkBlob = new Blob([chunksData[i] as unknown as BlobPart], { type: "video/mp4" });
            const chunkHash = await calculateSHA256(chunkBlob);

            const formData = new FormData();
            formData.append("uploadId", uploadId);
            formData.append("index", (i + 1).toString());
            formData.append("chunkHash", chunkHash);
            formData.append("file", chunkBlob, chunkName);

            setStatus(`Uploading chunk ${i + 1}/${chunkFiles.length}...`);

            const chunkRes = await fetch("http://localhost:8080/api/v1/upload/chunk", {
                method: "POST",
                body: formData
            });

            if (!chunkRes.ok) {
                setStatus(`Failed to upload chunk ${i + 1}`);
                // Cleanup
                chunksData.length = 0;
                return;
            }

            // We don't delete from FFmpeg here because we already read them into memory (chunksData).
            // But good practice to clean up FFmpeg FS if needed, though we read into memory so it's duplicated now.
            // If memory is tight, we should have hashed incrementally or re-read. 
            // For now, keeping in memory is simpler given we needed to concat for hash.
            await ffmpeg.deleteFile(chunkName);
            setProgress(((i + 1) / chunkFiles.length) * 100);
        }

        setStatus("Completing upload...");
        await fetch("http://localhost:8080/api/v1/upload/complete", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ uploadId })
        });

        setStatus("Upload complete!");
        await ffmpeg.deleteFile(inputName);
        chunksData.length = 0; // Clear memory
    };

    return (
        <div className="flex flex-col gap-4 p-4 border rounded-lg max-w-xl mx-auto mt-10">
            <h2 className="text-xl font-bold">FFmpeg File Uploader</h2>

            {!loaded ? (
                <button
                    onClick={load}
                    disabled={isLoading}
                    className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:bg-gray-400"
                >
                    {isLoading ? "Loading FFmpeg..." : "Load FFmpeg"}
                </button>
            ) : (
                <div className="flex flex-col gap-4">
                    <div className="text-green-600 font-semibold">FFmpeg is ready</div>
                    <input
                        type="file"
                        onChange={(e) => setFile(e.target.files?.item(0) || null)}
                        className="block w-full text-sm text-gray-500
                  file:mr-4 file:py-2 file:px-4
                  file:rounded-full file:border-0
                  file:text-sm file:font-semibold
                  file:bg-violet-50 file:text-violet-700
                  hover:file:bg-violet-100"
                    />

                    {file && (
                        <button
                            onClick={uploadFile}
                            className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
                        >
                            Upload & Segment
                        </button>
                    )}
                </div>
            )}

            {status && <div className="mt-2 text-sm text-gray-700">{status}</div>}

            {uploadId && (
                <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded text-sm text-green-800 break-all">
                    <strong>Upload ID:</strong> {uploadId}
                </div>
            )}

            {progress > 0 && (
                <div className="w-full bg-gray-200 rounded-full h-2.5 dark:bg-gray-700">
                    <div className="bg-blue-600 h-2.5 rounded-full" style={{ width: `${progress}%` }}></div>
                </div>
            )}

            <p ref={messageRef} className="text-xs text-gray-500 font-mono mt-2 h-20 overflow-y-auto border p-2 bg-gray-50"></p>
        </div>
    );
}
