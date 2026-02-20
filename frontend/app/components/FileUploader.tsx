"use client";

import { useState, useRef, useEffect } from "react";
import { FFmpeg } from "@ffmpeg/ffmpeg";
import { fetchFile, toBlobURL } from "@ffmpeg/util";

export default function FileUploader() {
    const [loaded, setLoaded] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [file, setFile] = useState<File | null>(null);
    const [thumbnail, setThumbnail] = useState<File | null>(null);
    const [customFilename, setCustomFilename] = useState("");
    const [fileContent, setFileContent] = useState("");
    const [progress, setProgress] = useState(0);
    const [status, setStatus] = useState("Initializing system...");
    const [uploadId, setUploadId] = useState("");

    const ffmpegRef = useRef<FFmpeg | null>(null);

    useEffect(() => {
        let isMounted = true;

        const loadFFmpeg = async () => {
            if (loaded) return;
            setIsLoading(true);
            const baseURL = "https://unpkg.com/@ffmpeg/core@0.12.6/dist/umd";

            if (!ffmpegRef.current) {
                ffmpegRef.current = new FFmpeg();
            }
            const ffmpeg = ffmpegRef.current;

            try {
                await ffmpeg.load({
                    coreURL: await toBlobURL(`${baseURL}/ffmpeg-core.js`, "text/javascript"),
                    wasmURL: await toBlobURL(`${baseURL}/ffmpeg-core.wasm`, "application/wasm"),
                });
                if (isMounted) {
                    setLoaded(true);
                    setStatus("");
                }
            } catch (error) {
                console.error("Failed to load FFmpeg", error);
                if (isMounted) setStatus("Failed to load processing engine. Please try again.");
            } finally {
                if (isMounted) setIsLoading(false);
            }
        };

        loadFFmpeg();

        return () => {
            isMounted = false;
        };
    }, []);

    const calculateSHA256 = async (blob: Blob): Promise<string> => {
        const buffer = await blob.arrayBuffer();
        const hashBuffer = await crypto.subtle.digest("SHA-256", buffer);
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        return hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");
    };

    const uploadFile = async () => {
        if (!file || !thumbnail || !loaded) return;
        const ffmpeg = ffmpegRef.current;
        if (!ffmpeg) return;

        setStatus("Segmenting video file...");
        setIsLoading(true);
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
            setIsLoading(false);
            return;
        }

        setStatus(`Found ${chunkFiles.length} chunks. Calculating total hash...`);

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

        const fullBuffer = new Uint8Array(totalSize);
        let offset = 0;
        for (const chunkData of chunksData) {
            fullBuffer.set(chunkData, offset);
            offset += chunkData.length;
        }

        const fileHash = await calculateSHA256(new Blob([fullBuffer]));

        setStatus("Initializing upload...");

        const initFormData = new FormData();
        initFormData.append("fileSize", totalSize.toString());
        initFormData.append("filename", customFilename || file.name);
        initFormData.append("filecontent", fileContent);
        initFormData.append("fileHash", fileHash);
        initFormData.append("totalChunks", chunkFiles.length.toString());
        initFormData.append("thumbnail", thumbnail);

        const initRes = await fetch("http://localhost:8080/api/v1/upload/init", {
            method: "POST",
            body: initFormData
        });

        if (!initRes.ok) {
            setStatus("Failed to initialize upload");
            chunksData.length = 0;
            setIsLoading(false);
            return;
        }

        const { uploadId } = await initRes.json();
        setUploadId(uploadId);
        setStatus("Starting upload...");

        for (let i = 0; i < chunkFiles.length; i++) {
            const chunkName = chunkFiles[i].name;
            const chunkBlob = new Blob([chunksData[i] as unknown as BlobPart], { type: "video/mp4" });
            const chunkHash = await calculateSHA256(chunkBlob);

            const formData = new FormData();
            formData.append("uploadId", uploadId);
            formData.append("index", (i + 1).toString());
            formData.append("chunkHash", chunkHash);
            formData.append("file", chunkBlob, chunkName);

            setStatus(`Uploading chunk ${i + 1} of ${chunkFiles.length}...`);

            const chunkRes = await fetch("http://localhost:8080/api/v1/upload/chunk", {
                method: "POST",
                body: formData
            });

            if (!chunkRes.ok) {
                setStatus(`Failed to upload chunk ${i + 1}`);
                chunksData.length = 0;
                setIsLoading(false);
                return;
            }

            await ffmpeg.deleteFile(chunkName);
            setProgress(((i + 1) / chunkFiles.length) * 100);
        }

        setStatus("Completing upload...");
        await fetch("http://localhost:8080/api/v1/upload/complete", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ uploadId })
        });

        setStatus("Upload complete successfully!");
        setIsLoading(false);
        await ffmpeg.deleteFile(inputName);
        chunksData.length = 0;
    };

    return (
        <div className="w-full">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Publicar Vídeo</h2>

            <div className="flex flex-col gap-5">
                {!loaded && isLoading ? (
                    <div className="flex flex-col items-center justify-center p-12 bg-gray-50 border border-gray-200 rounded-xl space-y-4">
                        <svg className="w-8 h-8 text-blue-500 animate-spin" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                        <p className="text-gray-600 font-medium">Carregando sistema de processamento...</p>
                        <p className="text-sm text-gray-400">Isso pode levar alguns instantes na primeira vez</p>
                    </div>
                ) : (
                    <>
                        <div className="flex flex-col gap-1.5">
                            <label className="text-sm font-semibold text-gray-800">Nome do Arquivo</label>
                            <input
                                type="text"
                                placeholder="Ex: Meu Primeiro Video.mp4"
                                value={customFilename}
                                onChange={(e) => setCustomFilename(e.target.value)}
                                className="w-full px-4 py-3 bg-white border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-shadow min-w-0"
                            />
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <label className="text-sm font-semibold text-gray-800">Descrição e Conteúdo</label>
                            <textarea
                                placeholder="Forneça os detalhes ou as letras relacionadas ao vídeo..."
                                value={fileContent}
                                onChange={(e) => setFileContent(e.target.value)}
                                className="w-full px-4 py-3 bg-white border border-gray-300 rounded-xl text-sm h-32 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-shadow resize-none"
                            />
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                            <div className="flex flex-col gap-1.5">
                                <label className="text-sm font-semibold text-gray-800">Arquivo de Video (MP4)</label>
                                <div className="border border-gray-300 bg-white rounded-xl px-2 py-2 flex items-center shadow-sm">
                                    <input
                                        type="file"
                                        accept="video/*"
                                        onChange={(e) => {
                                            const selectedFile = e.target.files?.item(0) || null;
                                            setFile(selectedFile);
                                            if (selectedFile && !customFilename) {
                                                setCustomFilename(selectedFile.name);
                                            }
                                        }}
                                        className="w-full text-sm text-gray-500 file:cursor-pointer file:mr-4 file:py-2.5 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 transition-colors"
                                    />
                                </div>
                            </div>

                            <div className="flex flex-col gap-1.5">
                                <label className="text-sm font-semibold text-gray-800">Imagem de Capa (Thumbnail)</label>
                                <div className="border border-gray-300 bg-white rounded-xl px-2 py-2 flex items-center shadow-sm">
                                    <input
                                        type="file"
                                        accept="image/*"
                                        onChange={(e) => {
                                            const selectedFile = e.target.files?.item(0) || null;
                                            setThumbnail(selectedFile);
                                        }}
                                        className="w-full text-sm text-gray-500 file:cursor-pointer file:mr-4 file:py-2.5 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 transition-colors"
                                    />
                                </div>
                                {thumbnail && (
                                    <div className="mt-2 w-full aspect-video rounded-xl border border-gray-200 overflow-hidden bg-black flex items-center justify-center relative shadow-sm">
                                        {/* eslint-disable-next-line @next/next/no-img-element */}
                                        <img
                                            src={URL.createObjectURL(thumbnail)}
                                            alt="Preview"
                                            className="w-full h-full object-contain"
                                            onLoad={(e) => URL.revokeObjectURL((e.target as HTMLImageElement).src)}
                                        />
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="mt-4 pt-4 border-t border-gray-100">
                            <button
                                onClick={uploadFile}
                                disabled={!file || !thumbnail || !customFilename.trim() || !fileContent.trim() || isLoading}
                                className="w-full sm:w-auto px-8 py-3.5 bg-blue-600 text-white rounded-xl font-semibold hover:bg-blue-700 disabled:bg-gray-300 disabled:text-gray-500 disabled:cursor-not-allowed transition-all shadow-sm flex items-center justify-center gap-2"
                            >
                                {isLoading && loaded ? (
                                    <>
                                        <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                                        Processando...
                                    </>
                                ) : "Realizar Upload"}
                            </button>
                        </div>

                        {status && (
                            <div className="mt-4 p-4 rounded-xl border flex items-center gap-3 text-sm bg-blue-50 border-blue-100 text-blue-800 shadow-sm">
                                <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" /></svg>
                                <span className="font-medium">{status}</span>
                            </div>
                        )}

                        {progress > 0 && progress < 100 && (
                            <div className="w-full bg-gray-100 rounded-full h-3 shadow-inner overflow-hidden border border-gray-200">
                                <div className="bg-blue-600 h-full transition-all duration-300 ease-out" style={{ width: `${progress}%` }}></div>
                            </div>
                        )}

                        {progress === 100 && !isLoading && (
                            <div className="w-full bg-green-500 h-3 shadow-inner rounded-full overflow-hidden border border-green-600"></div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}
