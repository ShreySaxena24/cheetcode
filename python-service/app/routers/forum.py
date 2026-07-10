from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from app.services.forum_scraper import scrape_leetcode_forum

router = APIRouter(prefix="/scrape", tags=["forum"])

class ForumScrapeRequest(BaseModel):
    question_slug: str
    max_posts: int = 5

class ScrapedItem(BaseModel):
    sourceUrl: str
    sourceTitle: str
    author: str
    rawContent: str
    metadata: dict
    status: str
    errorMessage: Optional[str] = None

@router.post("/forum", response_model=List[ScrapedItem])
async def scrape_forum(payload: ForumScrapeRequest):
    try:
        results = await scrape_leetcode_forum(payload.question_slug, payload.max_posts)
        return results
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
