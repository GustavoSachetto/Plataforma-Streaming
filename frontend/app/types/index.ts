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

export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
}

export interface CatalogSearchResponseDto {
    id: string;
    name: string;
    content: string;
    thumbnail: string;
}

export interface FileRead {
    id: string;
    filename: string;
    fileSize?: number;
    fileHash?: string;
    [key: string]: any;
}
