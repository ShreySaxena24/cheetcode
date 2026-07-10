import os
import logging
from typing import List, Dict
from googleapiclient.discovery import build

logger = logging.getLogger(__name__)

def scrape_youtube_search_fallback(query: str, max_results: int = 3) -> List[Dict[str, str]]:
    import requests
    import re
    import json
    
    search_query = f"LeetCode {query} solution"
    search_url = f"https://www.youtube.com/results?search_query={search_query.replace(' ', '+')}"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Accept-Language": "en-US,en;q=0.9"
    }
    
    try:
        response = requests.get(search_url, headers=headers, timeout=10)
        if response.status_code != 200:
            logger.warning(f"YouTube search page returned status code {response.status_code}")
            return []
            
        html = response.text
        match = re.search(r'var ytInitialData\s*=\s*({.*?});', html)
        if not match:
            match = re.search(r'window\["ytInitialData"\]\s*=\s*({.*?});', html)
            
        if not match:
            logger.warning("Could not find ytInitialData in YouTube response")
            return []
            
        json_text = match.group(1)
        data = json.loads(json_text)
        
        videos = []
        try:
            contents = data.get("contents", {}) \
                           .get("twoColumnSearchResultRenderer", {}) \
                           .get("primaryContents", {}) \
                           .get("sectionListRenderer", {}) \
                           .get("contents", [])
                           
            if not contents:
                return []
                
            item_section = contents[0].get("itemSectionRenderer", {})
            items = item_section.get("contents", [])
            
            for item in items:
                video_renderer = item.get("videoRenderer")
                if video_renderer:
                    video_id = video_renderer.get("videoId")
                    
                    title_runs = video_renderer.get("title", {}).get("runs", [])
                    title = title_runs[0].get("text") if title_runs else "Unknown Title"
                    
                    owner_runs = video_renderer.get("ownerText", {}).get("runs", [])
                    author = owner_runs[0].get("text") if owner_runs else "Unknown Channel"
                    
                    if video_id:
                        videos.append({
                            "video_id": video_id,
                            "title": title,
                            "author": author
                        })
                        if len(videos) >= max_results:
                            break
        except Exception as e:
            logger.error(f"Error parsing ytInitialData structure: {e}")
            
        return videos
    except Exception as e:
        logger.error(f"Error during YouTube search scraping: {e}")
        return []

def search_youtube_videos(query: str, max_results: int = 3) -> List[Dict[str, str]]:
    api_key = os.getenv("YOUTUBE_API_KEY")
    if not api_key:
        logger.info("YOUTUBE_API_KEY environment variable not set. Using keyless scraping fallback.")
        return scrape_youtube_search_fallback(query, max_results)
        
    try:
        youtube = build("youtube", "v3", developerKey=api_key)
        search_query = f"LeetCode {query} solution"
        
        request = youtube.search().list(
            q=search_query,
            part="snippet",
            maxResults=max_results,
            type="video"
        )
        response = request.execute()
        
        videos = []
        for item in response.get("items", []):
            video_id = item.get("id", {}).get("videoId")
            snippet = item.get("snippet", {})
            title = snippet.get("title")
            channel_title = snippet.get("channelTitle")
            
            if video_id:
                videos.append({
                    "video_id": video_id,
                    "title": title,
                    "author": channel_title
                })
        return videos
    except Exception as e:
        logger.error(f"Error performing YouTube API search: {e}. Falling back to keyless scraping.")
        return scrape_youtube_search_fallback(query, max_results)

