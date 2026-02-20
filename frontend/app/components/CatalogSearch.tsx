"use client";

import { useState } from "react";
import { FileRead } from "../types";

interface CatalogSearchProps {
    onSelectVideo: (videoId: string) => void;
}

export default function CatalogSearch({ onSelectVideo }: CatalogSearchProps) {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState<FileRead[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState("");
    const [hasSearched, setHasSearched] = useState(false);

    const handleSearch = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!query.trim()) return;

        setIsLoading(true);
        setError("");
        setHasSearched(true);

        try {
            // Trying with /api prefix first, as matches other endpoints (upload/download)
            let res = await fetch(`http://localhost:8080/api/v1/catalog/search?q=${encodeURIComponent(query)}`);

            // If 404, fallback to without /api
            if (res.status === 404) {
                res = await fetch(`http://localhost:8080/v1/catalog/search?q=${encodeURIComponent(query)}`);
            }

            if (!res.ok) {
                throw new Error("Failed to fetch catalog search results");
            }

            const data = await res.json();
            setResults(data);
        } catch (err: any) {
            console.error(err);
            setError(err.message || "An error occurred during search.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex flex-col gap-4 p-4 border rounded-lg w-full mt-10">
            <h2 className="text-xl font-bold">Catalog Search</h2>

            <form onSubmit={handleSearch} className="flex gap-2">
                <input
                    type="text"
                    placeholder="Search catalog..."
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    className="flex-1 p-2 border border-gray-300 rounded text-sm focus:ring-1 focus:ring-violet-500 focus:outline-none"
                />
                <button
                    type="submit"
                    disabled={isLoading || !query.trim()}
                    className="px-4 py-2 bg-indigo-500 text-white rounded hover:bg-indigo-600 disabled:bg-gray-400 font-semibold"
                >
                    {isLoading ? "Searching..." : "Search"}
                </button>
            </form>

            {error && <div className="text-red-500 text-sm mt-2">{error}</div>}

            {hasSearched && !isLoading && results.length === 0 && !error && (
                <div className="text-gray-500 text-sm mt-2">No results found for "{query}".</div>
            )}

            {results.length > 0 && (
                <div className="mt-4 flex flex-col gap-2 max-h-64 overflow-y-auto">
                    {results.map((file) => {
                        // Handle potential property name mismatches from backend
                        const id = file.id || file.fileId || (file as any).videoId;
                        const name = file.filename || file.fileName || (file as any).name || "Unknown File";

                        return (
                            <div key={id} className="flex justify-between items-center p-3 border rounded hover:bg-gray-50">
                                <div>
                                    <div className="font-semibold text-sm">{name}</div>
                                    <div className="text-xs text-gray-400">ID: {id}</div>
                                </div>
                                <button
                                    onClick={() => onSelectVideo(id)}
                                    className="px-3 py-1 bg-green-500 text-white rounded text-sm hover:bg-green-600"
                                >
                                    Play
                                </button>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
