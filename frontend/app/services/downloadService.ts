import { ManifestResponseDto } from "../types";

const BASE_URL = "http://localhost:8080/api/v1/download";

export const downloadService = {
    getManifest: async (uploadId: string): Promise<ManifestResponseDto> => {
        const response = await fetch(`${BASE_URL}/${uploadId}`);
        if (!response.ok) {
            throw new Error("Failed to fetch manifest");
        }
        return response.json();
    },

    getChunkUrl: (uploadId: string, index: number) => {
        return `${BASE_URL}/${uploadId}/chunk/${index}`;
    },

    fetchChunk: async (url: string): Promise<ArrayBuffer> => {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Failed to fetch chunk: ${url}`);
        }
        return response.arrayBuffer();
    }
};
