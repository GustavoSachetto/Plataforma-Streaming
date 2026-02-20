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

export interface FileRead {
    id: string;
    filename: string;
    // adding optional fields just in case
    fileSize?: number;
    fileHash?: string;
    [key: string]: any;
}
