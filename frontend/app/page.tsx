"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import VideoPlayer from "./components/VideoPlayer";
import CatalogSearch from "./components/CatalogSearch";
import { CatalogSearchResponseDto } from "./types";

export default function Home() {
  const [selectedVideo, setSelectedVideo] = useState<CatalogSearchResponseDto | null>(null);
  const router = useRouter();

  if (!selectedVideo) {
    return (
      <main className="flex min-h-screen flex-col items-center px-8 py-12 bg-gray-50 border-t-4 border-blue-600">
        <CatalogSearch onSelectVideo={setSelectedVideo} onPublishVideo={() => router.push('/upload')} />
      </main>
    )
  }

  return (
    <main className="flex min-h-screen flex-col px-8 py-12 gap-8 bg-gray-50 border-t-4 border-blue-600">
      <div className="w-full max-w-7xl mx-auto flex flex-col lg:flex-row gap-8">

        {/* Left Column - Video Player & Info */}
        <div className="flex-1 lg:w-2/3 flex flex-col gap-4">

          <div className="flex items-center gap-4 mb-2">
            <button onClick={() => setSelectedVideo(null)} className="flex items-center gap-2 text-sm text-gray-600 hover:text-blue-600 transition-colors bg-white px-3 py-1.5 rounded-full border border-gray-200 shadow-sm">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
              Voltar para o Catálogo
            </button>
          </div>

          <div className="w-full bg-black rounded-xl overflow-hidden shadow-lg border border-gray-200">
            <VideoPlayer uploadId={selectedVideo.id} />
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 mb-8 lg:mb-0 relative">
            <div className="flex justify-between items-start gap-4">
              <div>
                <h1 className="text-2xl font-bold text-gray-900 leading-tight">{selectedVideo.name}</h1>
                <p className="text-sm text-gray-500 mt-2 flex items-center gap-2">
                  Gustavo Sachetto | 07/08/2026 - 12:55
                </p>
              </div>
              <button
                onClick={async (e) => {
                  const btn = e.currentTarget;
                  const originalContent = btn.innerHTML;
                  btn.innerHTML = '<svg class="w-6 h-6 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>';
                  btn.disabled = true;
                  btn.classList.add('opacity-50', 'cursor-not-allowed');

                  try {
                    const response = await fetch(`http://localhost:8080/api/v1/download/${selectedVideo.id}/export`);
                    if (!response.ok) throw new Error('Export failed');

                    const blob = await response.blob();
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    // Determine filename from content-disposition header if available, otherwise fallback
                    const disposition = response.headers.get('content-disposition');
                    let filename = `${selectedVideo.name}.mp4`;
                    if (disposition && disposition.includes('filename=')) {
                      filename = disposition.split('filename=')[1].replace(/"/g, '');
                    }
                    a.download = filename;
                    document.body.appendChild(a);
                    a.click();
                    a.remove();
                    window.URL.revokeObjectURL(url);
                  } catch (error) {
                    console.error("Export error:", error);
                    alert("Ocorreu um erro ao exportar o vídeo.");
                  } finally {
                    btn.innerHTML = originalContent;
                    btn.disabled = false;
                    btn.classList.remove('opacity-50', 'cursor-not-allowed');
                  }
                }}
                className="p-3 bg-blue-50 text-blue-600 hover:bg-blue-100 hover:text-blue-700 rounded-full transition-colors flex-shrink-0 group"
                title="Exportar Vídeo"
              >
                <svg className="w-6 h-6 group-hover:-translate-y-0.5 transition-transform" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
              </button>
            </div>

            <div className="mt-6 pt-6 border-t border-gray-100">
              <p className="text-gray-700 whitespace-pre-wrap">{selectedVideo.content || "Sem descrição"}</p>
            </div>
          </div>
        </div>

        {/* Right Column - Catalog List */}
        <div className="w-full lg:w-1/3 lg:max-w-sm flex flex-col">
          <h3 className="font-semibold text-lg text-gray-900 mb-4 px-2">Mais vídeos</h3>
          <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-200">
            <CatalogSearch compact={true} onSelectVideo={setSelectedVideo} excludeId={selectedVideo.id} />
          </div>
        </div>

      </div>
    </main>
  );
}
