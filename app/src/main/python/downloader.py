import yt_dlp
import os
import re

def get_video_info(url):
    """
    Extracts video title and thumbnail URL.
    """
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
    }
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            return {
                "title": info.get('title', 'Unknown Title'),
                "thumbnail": info.get('thumbnail', ''),
                "id": info.get('id', ''),
                "duration": info.get('duration', 0)
            }
    except Exception as e:
        return {"error": str(e)}

def download_video(url, download_path, progress_callback):
    """
    Downloads the video with progress updates.
    """
    def ydl_progress_hook(d):
        if d['status'] == 'downloading':
            p = d.get('_percent_str', '0%')
            # Remove ANSI escape codes and percentage sign
            p = re.sub(r'\x1b\[[0-9;]*m', '', p).replace('%', '').strip()
            try:
                progress_callback(float(p))
            except ValueError:
                pass
        elif d['status'] == 'finished':
            progress_callback(100.0)

    ydl_opts = {
        'format': 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
        'outtmpl': os.path.join(download_path, 'Shorts_%(title)s_%(id)s.%(ext)s'),
        'progress_hooks': [ydl_progress_hook],
        'merge_output_format': 'mp4',
        'quiet': True,
        'no_warnings': True,
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])
        return {"success": True}
    except Exception as e:
        return {"error": str(e)}

# For testing purposes
if __name__ == "__main__":
    def test_callback(p):
        print(f"Progress: {p}%")
    
    test_url = "https://www.youtube.com/shorts/dQw4w9WgXcQ" # Not a short but for testing
    print(get_video_info(test_url))
