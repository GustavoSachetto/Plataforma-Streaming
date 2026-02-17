export interface ChunkIndexResponseDto {
    index: number;
    hash: string;
}

export interface ManifestResponseDto {
    fileId: string;
    fileName: string;
    fileSize: number;
    fileHash: string;
    chunks: ChunkIndexResponseDto[];
}
