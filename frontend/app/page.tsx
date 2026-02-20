"use client";

import { useState } from "react";
import FileUploader from "./components/FileUploader";
import VideoPlayer from "./components/VideoPlayer";
import CatalogSearch from "./components/CatalogSearch";

export default function Home() {
  const [videoId, setVideoId] = useState("");

  return (
    <main className="flex min-h-screen flex-col items-center p-24 gap-8">
      <div className="z-10 w-full max-w-5xl items-start justify-between font-mono text-sm lg:flex gap-8">
        <div className="flex-1 w-full">
          <h1 className="text-2xl font-bold mb-4">Upload Video</h1>
          <FileUploader />

          <CatalogSearch onSelectVideo={(id) => setVideoId(id)} />
        </div>

        <div className="flex-1 w-full flex flex-col gap-4">
          <h1 className="text-2xl font-bold mb-4">Play Video</h1>
          <div className="flex flex-col gap-2">
            <label className="text-sm font-semibold">Enter Upload ID:</label>
            <input
              type="text"
              placeholder="UUID..."
              className="p-2 border rounded text-black"
              value={videoId}
              onChange={(e) => setVideoId(e.target.value)}
            />
          </div>

          {videoId && <VideoPlayer uploadId={videoId} />}
        </div>
      </div>
    </main>
  );
}
