import logging
from typing import Dict, Any
from youtube_transcript_api import YouTubeTranscriptApi

logger = logging.getLogger(__name__)

def fetch_single_video_transcript(video_id: str) -> Dict[str, Any]:
    source_url = f"https://www.youtube.com/watch?v={video_id}"
    try:
        # Fetch transcript
        transcript_list = YouTubeTranscriptApi.get_transcript(video_id, languages=['en'])
        raw_content = " ".join([item['text'] for item in transcript_list])
        
        return {
            "sourceUrl": source_url,
            "sourceTitle": f"YouTube Video {video_id}",
            "author": "YouTube Creator",
            "rawContent": raw_content,
            "metadata": {"video_id": video_id},
            "status": "SUCCESS"
        }
    except Exception as e:
        logger.warning(f"Failed to fetch transcript for video {video_id}: {e}")
        return {
            "sourceUrl": source_url,
            "sourceTitle": f"YouTube Video {video_id}",
            "author": "YouTube Creator",
            "rawContent": "",
            "metadata": {"video_id": video_id},
            "status": "FAILED",
            "errorMessage": str(e)
        }
