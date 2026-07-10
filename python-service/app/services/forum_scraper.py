import httpx
import logging
from typing import List, Dict, Any

logger = logging.getLogger(__name__)

async def scrape_leetcode_forum(question_slug: str, max_posts: int = 5) -> List[Dict[str, Any]]:
    url = "https://leetcode.com/graphql"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Content-Type": "application/json",
        "Referer": "https://leetcode.com"
    }

    # Query for community solutions
    graphql_query = """
    query questionTopicsList($questionSlug: String!, $first: Int, $skip: Int, $query: String, $orderBy: TopicSortingOption) {
      questionTopicsList(questionSlug: $questionSlug, first: $first, skip: $skip, query: $query, orderBy: $orderBy) {
        edges {
          node {
            id
            title
            post {
              id
              content
              author {
                username
              }
            }
            commentCount
            viewCount
            numUpvotes
          }
        }
      }
    }
    """

    variables = {
        "questionSlug": question_slug,
        "first": max_posts,
        "skip": 0,
        "query": "",
        "orderBy": "most_votes"
    }

    payload = {
        "query": graphql_query,
        "variables": variables
    }

    results = []
    
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(url, json=payload, headers=headers)
            if response.status_code != 200:
                logger.error(f"LeetCode GraphQL returned status {response.status_code}")
                return []
                
            data = response.json()
            if "errors" in data:
                logger.error(f"GraphQL Errors: {data['errors']}")
                return []
                
            edges = data.get("data", {}).get("questionTopicsList", {}).get("edges", [])
            for edge in edges:
                node = edge.get("node", {})
                node_id = node.get("id")
                title = node.get("title")
                post = node.get("post", {})
                content = post.get("content", "")
                author = post.get("author", {}).get("username", "Unknown")
                
                source_url = f"https://leetcode.com/discuss/topic/{node_id}"
                
                metadata = {
                    "topic_id": node_id,
                    "comment_count": node.get("commentCount"),
                    "view_count": node.get("viewCount"),
                    "upvotes": node.get("numUpvotes")
                }
                
                results.append({
                    "sourceUrl": source_url,
                    "sourceTitle": title,
                    "author": author,
                    "rawContent": content,
                    "metadata": metadata,
                    "status": "SUCCESS"
                })
                
    except Exception as e:
        logger.error(f"Failed to scrape forum for {question_slug}: {e}")
        results.append({
            "sourceUrl": f"https://leetcode.com/problems/{question_slug}/discuss",
            "sourceTitle": "LeetCode Discussion Forum",
            "author": "LeetCode System",
            "rawContent": "",
            "metadata": {},
            "status": "FAILED",
            "errorMessage": str(e)
        })

    return results[:max_posts]
