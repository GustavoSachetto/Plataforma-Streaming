"use client";

import { useEffect, useRef, useState } from "react";
import Hls from "hls.js";

interface VideoPlayerProps {
    uploadId: string;
}

export default function VideoPlayer({ uploadId }: VideoPlayerProps) {
    const videoRef = useRef<HTMLVideoElement>(null);
    const [error, setError] = useState<string>("");

    useEffect(() => {
        const video = videoRef.current;
        if (!video) return;

        const src = `http://localhost:8080/api/v1/download/${uploadId}/playlist.m3u8`;
        let hls: Hls | null = null;

        const handlePlay = () => {
            if (hls) {
                hls.startLoad();
            }
        };

        if (Hls.isSupported()) {
            hls = new Hls({
                maxBufferLength: 15,
                maxMaxBufferLength: 20,
                autoStartLoad: false,
            });
            hls.loadSource(src);
            hls.attachMedia(video);

            hls.on(Hls.Events.MANIFEST_PARSED, () => {
                // Video is ready to be played, but we wait for user interaction
                console.log("Manifest parsed, waiting for user to play...");
            });

            video.addEventListener('play', handlePlay);

            hls.on(Hls.Events.ERROR, (event, data) => {
                if (data.fatal) {
                    switch (data.type) {
                        case Hls.ErrorTypes.NETWORK_ERROR:
                            console.error("fatal network error encountered, try to recover");
                            hls?.startLoad();
                            break;
                        case Hls.ErrorTypes.MEDIA_ERROR:
                            console.error("fatal media error encountered, try to recover");
                            hls?.recoverMediaError();
                            break;
                        default:
                            console.error("fatal error, cannot recover");
                            hls?.destroy();
                            break;
                    }
                    setError(`Playback Error: ${data.details}`);
                }
            });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            // Native HLS support (Safari)
            video.src = src;
            video.addEventListener('loadedmetadata', () => {
                video.play().catch(e => console.error("Error playing video:", e));
            });
        } else {
            console.error("HLS is not supported in this browser.");
            // Use setTimeout to avoid strictly synchronous state update warning in useEffect
            setTimeout(() => {
                setError("HLS is not supported in this browser.");
            }, 0);
        }

        return () => {
            if (hls) {
                hls.destroy();
            }
            if (video) {
                video.removeEventListener('play', handlePlay);
            }
        };
    }, [uploadId]);

    return (
        <div className="flex flex-col gap-4 p-4 border rounded-lg max-w-xl mx-auto mt-10">
            <div className="flex justify-between items-center">
                <h2 className="text-xl font-bold">Streaming Player</h2>
                <button
                    onClick={() => window.open(`http://localhost:8080/api/v1/download/${uploadId}/export`, '_blank')}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded transition-colors"
                >
                    Export Video
                </button>
            </div>
            {error && <div className="text-red-500">{error}</div>}
            <video
                ref={videoRef}
                controls
                playsInline
                width="100%"
                className="w-full bg-black rounded shadow-lg"
            />
        </div>
    );
}
