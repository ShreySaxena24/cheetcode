import os
from fastapi import FastAPI
from app.routers import youtube, forum

app = FastAPI(title="CheetCode Scraper Service", version="1.0")

# Include routers
app.include_router(youtube.router)
app.include_router(forum.router)

@app.get("/")
def read_root():
    return {"message": "CheetCode Scraper Service is running"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
