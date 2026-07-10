from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from app.services.youtube_search import search_youtube_videos
from app.services.transcript_fetcher import fetch_single_video_transcript

router = APIRouter(prefix="/scrape/youtube", tags=["youtube"])

class YoutubeSearchRequest(BaseModel):
    question_title: str
    max_results: int = 3

class YoutubeSearchResult(BaseModel):
    video_id: str
    title: str
    author: str

class YoutubeTranscriptRequest(BaseModel):
    video_id: str

class ScrapedTranscriptItem(BaseModel):
    sourceUrl: str
    sourceTitle: str
    author: str
    rawContent: str
    metadata: dict
    status: str
    errorMessage: Optional[str] = None

@router.post("/search", response_model=List[YoutubeSearchResult])
async def search_youtube(payload: YoutubeSearchRequest):
    try:
        results = search_youtube_videos(payload.question_title, payload.max_results)
        # Map fields to match YoutubeSearchResult
        mapped_results = []
        for r in results:
            mapped_results.append(YoutubeSearchResult(
                video_id=r.get("video_id"),
                title=r.get("title"),
                author=r.get("author")
            ))
        return mapped_results
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/transcript", response_model=ScrapedTranscriptItem)
async def fetch_transcript(payload: YoutubeTranscriptRequest):
    try:
        result = fetch_single_video_transcript(payload.video_id)
        return ScrapedTranscriptItem(
            sourceUrl=result.get("sourceUrl"),
            sourceTitle=result.get("sourceTitle"),
            author=result.get("author"),
            rawContent=result.get("rawContent"),
            metadata=result.get("metadata"),
            status=result.get("status"),
            errorMessage=result.get("errorMessage")
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
