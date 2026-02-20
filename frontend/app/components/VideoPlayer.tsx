"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import Hls from "hls.js";

interface VideoPlayerProps {
    uploadId: string;
}

const formatTime = (timeInSeconds: number) => {
    if (isNaN(timeInSeconds)) return "0:00";
    const minutes = Math.floor(timeInSeconds / 60);
    const seconds = Math.floor(timeInSeconds % 60);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
};

export default function VideoPlayer({ uploadId }: VideoPlayerProps) {
    const videoRef = useRef<HTMLVideoElement>(null);
    const playerContainerRef = useRef<HTMLDivElement>(null);
    const [error, setError] = useState<string>("");

    const [isPlaying, setIsPlaying] = useState(false);
    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [bufferedProgress, setBufferedProgress] = useState(0);
    const [volume, setVolume] = useState(1);
    const [isMuted, setIsMuted] = useState(false);
    const [isFullscreen, setIsFullscreen] = useState(false);
    const [showControls, setShowControls] = useState(true);
    let controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);

    // ... (HLS useEffect remains unchanged) ...
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
                // Manifest parsed
            });

            video.addEventListener('play', handlePlay);

            hls.on(Hls.Events.ERROR, (event, data) => {
                if (data.fatal) {
                    switch (data.type) {
                        case Hls.ErrorTypes.NETWORK_ERROR:
                            hls?.startLoad();
                            break;
                        case Hls.ErrorTypes.MEDIA_ERROR:
                            hls?.recoverMediaError();
                            break;
                        default:
                            hls?.destroy();
                            break;
                    }
                    setError(`Playback Error: ${data.details}`);
                }
            });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            video.src = src;
        } else {
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

    // Custom Controls Logic
    useEffect(() => {
        const video = videoRef.current;
        if (!video) return;

        const updateBuffer = () => {
            if (video.buffered.length > 0) {
                // Get the furthest buffered range
                let maxBuffered = 0;
                for (let i = 0; i < video.buffered.length; i++) {
                    if (video.buffered.end(i) > maxBuffered) {
                        maxBuffered = video.buffered.end(i);
                    }
                }
                setBufferedProgress(maxBuffered);
            }
        };

        const handleTimeUpdate = () => {
            setCurrentTime(video.currentTime);
            updateBuffer();
        };
        const handleProgress = () => updateBuffer();
        const handleLoadedMetadata = () => setDuration(video.duration);
        const handlePlayEvent = () => setIsPlaying(true);
        const handlePauseEvent = () => setIsPlaying(false);
        const handleVolumeChange = () => {
            setVolume(video.volume);
            setIsMuted(video.muted);
        };

        video.addEventListener('timeupdate', handleTimeUpdate);
        video.addEventListener('progress', handleProgress);
        video.addEventListener('loadedmetadata', handleLoadedMetadata);
        video.addEventListener('play', handlePlayEvent);
        video.addEventListener('pause', handlePauseEvent);
        video.addEventListener('volumechange', handleVolumeChange);

        return () => {
            video.removeEventListener('timeupdate', handleTimeUpdate);
            video.removeEventListener('progress', handleProgress);
            video.removeEventListener('loadedmetadata', handleLoadedMetadata);
            video.removeEventListener('play', handlePlayEvent);
            video.removeEventListener('pause', handlePauseEvent);
            video.removeEventListener('volumechange', handleVolumeChange);
        };
    }, []);

    const togglePlay = () => {
        if (videoRef.current) {
            if (isPlaying) videoRef.current.pause();
            else videoRef.current.play();
        }
    };

    const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newTime = Number(e.target.value);
        if (videoRef.current) {
            videoRef.current.currentTime = newTime;
            setCurrentTime(newTime);
        }
    };

    const toggleMute = () => {
        if (videoRef.current) {
            videoRef.current.muted = !isMuted;
        }
    };

    const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newVol = Number(e.target.value);
        if (videoRef.current) {
            videoRef.current.volume = newVol;
            videoRef.current.muted = newVol === 0;
        }
    };

    const toggleFullscreen = () => {
        if (!document.fullscreenElement) {
            playerContainerRef.current?.requestFullscreen().catch(() => { });
        } else {
            document.exitFullscreen();
        }
    };

    useEffect(() => {
        const handleFullscreenChange = () => {
            setIsFullscreen(!!document.fullscreenElement);
        };
        document.addEventListener('fullscreenchange', handleFullscreenChange);
        return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
    }, []);

    const handleMouseMove = () => {
        setShowControls(true);
        if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
        controlsTimeoutRef.current = setTimeout(() => {
            if (isPlaying) setShowControls(false);
        }, 2500);
    };

    const handleMouseLeave = () => {
        if (isPlaying) setShowControls(false);
    };

    return (
        <div className="w-full h-full flex justify-center bg-black rounded-lg overflow-hidden">
            {error && <div className="absolute top-4 left-4 z-50 text-red-500 bg-white/80 p-2 rounded">{error}</div>}

            <div
                ref={playerContainerRef}
                className="relative w-full aspect-video bg-black group"
                onMouseMove={handleMouseMove}
                onMouseLeave={handleMouseLeave}
                onClick={togglePlay}
            >
                <video
                    ref={videoRef}
                    playsInline
                    className="w-full h-full cursor-pointer"
                    style={{ objectFit: 'contain' }}
                />

                {/* Gradient Overlay for Controls */}
                <div className={`absolute bottom-0 left-0 right-0 h-1/2 bg-gradient-to-t from-black/80 via-black/40 to-transparent pointer-events-none transition-opacity duration-300 ${showControls || !isPlaying ? 'opacity-100' : 'opacity-0'}`}></div>

                {/* Controls */}
                <div
                    className={`absolute bottom-0 left-0 right-0 px-4 pb-4 pt-8 flex flex-col gap-2 transition-opacity duration-300 ${showControls || !isPlaying ? 'opacity-100' : 'opacity-0'}`}
                    onClick={(e) => e.stopPropagation()}
                >

                    {/* Progress Bar */}
                    <div className="w-full group/progress flex items-center h-4 cursor-pointer relative">
                        {/* Buffered Progress */}
                        <div
                            className="absolute left-0 top-1/2 -translate-y-1/2 h-1 bg-gray-500 pointer-events-none transition-all group-hover/progress:h-1.5 rounded-full"
                            style={{ width: `${(bufferedProgress / (duration || 1)) * 100}%` }}
                        ></div>
                        <input
                            type="range"
                            min={0}
                            max={duration || 100}
                            value={currentTime}
                            onChange={handleSeek}
                            className="w-full h-1 bg-white/20 appearance-none rounded-full outline-none [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:bg-blue-500 [&::-webkit-slider-thumb]:rounded-full cursor-pointer relative z-10 transition-all group-hover/progress:h-1.5"
                            style={{
                                background: `linear-gradient(to right, #3b82f6 ${(currentTime / (duration || 1)) * 100}%, transparent ${(currentTime / (duration || 1)) * 100}%)`
                            }}
                        />
                    </div>

                    {/* Control Buttons */}
                    <div className="flex items-center justify-between mt-1 text-white">
                        <div className="flex items-center gap-4">
                            <button onClick={togglePlay} className="hover:text-blue-400 transition-colors focus:outline-none">
                                {isPlaying ? (
                                    <svg className="w-6 h-6 fill-current" viewBox="0 0 24 24"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" /></svg>
                                ) : (
                                    <svg className="w-6 h-6 fill-current" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
                                )}
                            </button>

                            <div className="text-sm font-medium tracking-wide">
                                {formatTime(currentTime)} / {formatTime(duration)}
                            </div>
                        </div>

                        <div className="flex items-center gap-4">
                            <div className="flex items-center gap-2 group/vol">
                                <button onClick={toggleMute} className="hover:text-blue-400 transition-colors focus:outline-none">
                                    {isMuted || volume === 0 ? (
                                        <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z" /></svg>
                                    ) : volume < 0.5 ? (
                                        <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M5 9v6h4l5 5V4L9 9H5zm11.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z" /></svg>
                                    ) : (
                                        <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z" /></svg>
                                    )}
                                </button>
                                <input
                                    type="range"
                                    min={0} max={1} step={0.05}
                                    value={isMuted ? 0 : volume}
                                    onChange={handleVolumeChange}
                                    className="w-0 opacity-0 group-hover/vol:w-20 group-hover/vol:opacity-100 transition-all duration-300 h-1 bg-white/30 appearance-none rounded-full outline-none [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-2.5 [&::-webkit-slider-thumb]:h-2.5 [&::-webkit-slider-thumb]:bg-blue-500 [&::-webkit-slider-thumb]:rounded-full cursor-pointer"
                                    style={{
                                        background: `linear-gradient(to right, #3b82f6 ${(isMuted ? 0 : volume) * 100}%, rgba(255,255,255,0.3) ${(isMuted ? 0 : volume) * 100}%)`
                                    }}
                                />
                            </div>

                            <button className="hover:text-blue-400 transition-colors focus:outline-none">
                                <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M12 15.5A3.5 3.5 0 018.5 12 3.5 3.5 0 0112 8.5a3.5 3.5 0 013.5 3.5 3.5 3.5 0 01-3.5 3.5m7.43-2.53c.04-.32.07-.64.07-.97 0-.33-.03-.66-.07-.97l2.11-1.66c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.31-.61-.22l-2.49 1c-.52-.39-1.06-.73-1.69-.98l-.38-2.65A.488.488 0 0014 2h-4c-.25 0-.46.18-.5.42l-.38 2.65c-.63.25-1.17.59-1.69.98l-2.49-1c-.22-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49-.12-.64l2.11 1.66c-.04.31-.07.65-.07.97 0 .33.03.66.07.97l-2.11 1.66c-.19.15-.24.42-.12.64l2 3.46c.12.22.39.31.61.22l2.49-1c.52.39 1.06.73 1.69.98l.38 2.65c.04.24.25.42.5.42h4c.25 0 .46-.18.5-.42l.38-2.65c.63-.25 1.17-.59 1.69-.98l2.49 1c.22.09.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.66z" /></svg>
                            </button>

                            <button onClick={toggleFullscreen} className="hover:text-blue-400 transition-colors focus:outline-none">
                                {isFullscreen ? (
                                    <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z" /></svg>
                                ) : (
                                    <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z" /></svg>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
