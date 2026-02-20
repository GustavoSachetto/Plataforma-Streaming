"use client";

import { useState, useEffect, useCallback } from "react";
import { CatalogSearchResponseDto, PageResponse } from "../types";

interface CatalogSearchProps {
    onSelectVideo: (video: CatalogSearchResponseDto) => void;
    onPublishVideo?: () => void;
    compact?: boolean;
    excludeId?: string;
}

export default function CatalogSearch({ onSelectVideo, onPublishVideo, compact = false, excludeId }: CatalogSearchProps) {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState<CatalogSearchResponseDto[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState("");
    const [hasSearched, setHasSearched] = useState(false);

    const fetchLatest = useCallback(async () => {
        setIsLoading(true);
        setError("");

        try {
            let res = await fetch(`http://localhost:8080/api/v1/catalog/latest`);
            if (res.status === 404) {
                res = await fetch(`http://localhost:8080/v1/catalog/latest`);
            }
            if (!res.ok) throw new Error("Failed to fetch latest videos");
            const data: PageResponse<CatalogSearchResponseDto> = await res.json();
            setResults(data.content || []);
        } catch (err: any) {
            console.error(err);
            setError(err.message || "An error occurred.");
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchLatest();
    }, [fetchLatest]);

    const handleSearch = async (e?: React.FormEvent) => {
        if (e) e.preventDefault();

        if (!query.trim()) {
            setHasSearched(false);
            return fetchLatest();
        }

        setIsLoading(true);
        setError("");
        setHasSearched(true);

        try {
            // Try with /api prefix first
            let res = await fetch(`http://localhost:8080/api/v1/catalog/search?q=${encodeURIComponent(query)}&page=0&size=10`);

            // Fallback
            if (res.status === 404) {
                res = await fetch(`http://localhost:8080/v1/catalog/search?q=${encodeURIComponent(query)}&page=0&size=10`);
            }

            if (!res.ok) {
                throw new Error("Failed to fetch catalog search results");
            }

            const data: PageResponse<CatalogSearchResponseDto> = await res.json();
            setResults(data.content || []);
        } catch (err: any) {
            console.error(err);
            setError(err.message || "An error occurred during search.");
        } finally {
            setIsLoading(false);
        }
    };

    const filteredResults = excludeId ? results.filter(v => v.id !== excludeId) : results;

    return (
        <div className={`w-full ${compact ? '' : 'max-w-5xl mx-auto my-8'} font-sans`}>
            {/* Top Bar: Search and Publish */}
            {!compact && (
                <div className="flex items-center justify-between gap-4 mb-8">
                    <form onSubmit={handleSearch} className="flex-1 max-w-3xl flex items-center border border-gray-400 rounded-full px-4 py-2 bg-white focus-within:ring-2 focus-within:ring-blue-500 focus-within:border-blue-500 transition-all">
                        <input
                            type="text"
                            placeholder="Pesquisar..."
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            className="flex-1 outline-none text-sm text-gray-800 bg-transparent"
                        />
                        <button type="submit" disabled={isLoading} className="text-gray-500 hover:text-gray-700 ml-2">
                            {isLoading ? (
                                <div className="w-5 h-5 border-2 border-gray-400 border-t-transparent rounded-full animate-spin"></div>
                            ) : (
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                </svg>
                            )}
                        </button>
                    </form>

                    <button
                        onClick={onPublishVideo}
                        className="px-5 py-2.5 bg-blue-50 text-blue-600 hover:bg-blue-100 hover:text-blue-700 rounded-full transition-colors flex items-center gap-2 group flex-shrink-0"
                        title="Publicar Vídeo"
                    >
                        <svg className="w-5 h-5 group-hover:-translate-y-0.5 transition-transform" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
                        </svg>
                        <span className="font-semibold text-sm hidden sm:inline">Publicar</span>
                    </button>
                </div>
            )}

            {error && <div className="text-red-500 text-sm mb-4">{error}</div>}

            {hasSearched && !isLoading && filteredResults.length === 0 && !error && (
                <div className="text-gray-500 text-sm mt-8 text-center">Nenhum resultado encontrado para "{query}".</div>
            )}

            {/* Video Grid */}
            {filteredResults.length > 0 && (
                <div className={compact ? "flex flex-col gap-6" : "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-8"}>
                    {filteredResults.map((video) => {
                        // Ensure thumbnail has correct base64 prefix if it's sent as raw base64 string
                        const imgSrc = video.thumbnail;

                        return (
                            <div
                                key={video.id}
                                className={`flex ${compact ? 'flex-row items-start gap-4' : 'flex-col gap-3'} cursor-pointer group`}
                                onClick={() => onSelectVideo(video)}
                            >
                                <div className={`${compact ? 'w-40 flex-shrink-0' : ''} aspect-video bg-gray-100 rounded-xl border border-gray-200 overflow-hidden shadow-sm`}>
                                    {imgSrc ? (
                                        <img
                                            src={imgSrc}
                                            alt={video.name}
                                            crossOrigin="anonymous"
                                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                                        />
                                    ) : (
                                        <div className="w-full h-full flex flex-col items-center justify-center text-gray-400 gap-2">
                                            <svg className="w-8 h-8 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                                        </div>
                                    )}
                                </div>
                                <div>
                                    <h3 className="font-semibold text-[15px] text-gray-900 leading-snug line-clamp-2 group-hover:text-blue-600 transition-colors">
                                        {video.name}
                                    </h3>
                                    <p className="text-xs text-gray-500 mt-1 line-clamp-1">
                                        {video.content || "Sem descrição"}
                                    </p>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
