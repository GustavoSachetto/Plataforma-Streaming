"use client";

import Link from "next/link";
import FileUploader from "../components/FileUploader";

export default function UploadPage() {
    return (
        <main className="flex min-h-screen flex-col items-center p-24 font-sans bg-gray-50">
            <div className="w-full max-w-2xl mb-6 flex justify-between items-center bg-white p-4 rounded-xl border border-gray-200 shadow-sm mx-auto">
                <Link href="/" className="text-blue-600 hover:underline flex items-center gap-1 font-medium">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
                    Voltar para o Catálogo
                </Link>
                <h1 className="text-xl font-bold">Publicar Vídeo</h1>
            </div>
            <div className="w-full max-w-2xl mx-auto">
                <FileUploader />
            </div>
        </main>
    );
}
