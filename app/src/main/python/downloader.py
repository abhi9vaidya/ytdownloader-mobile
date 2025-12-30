import yt_dlp
import os
import re


def _sanitize_filename(s: str) -> str:
    # Remove filesystem-unfriendly characters and trim length
    s = re.sub(r"[\\/:*?\"<>|]", "_", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s[:120]


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


def download_video(url, download_path, progress_callback=None):
    """
    Downloads the video with optional progress updates. progress_callback(progress_float)
    If progress_callback is None, function still works and returns a result dict.
    """
    def ydl_progress_hook(d):
        try:
            if d.get('status') == 'downloading':
                p = d.get('_percent_str', '0%')
                p = re.sub(r'\x1b\[[0-9;]*m', '', p).replace('%', '').strip()
                try:
                    if progress_callback:
                        progress_callback(float(p))
                except ValueError:
                    pass
            elif d.get('status') == 'finished':
                if progress_callback:
                    progress_callback(100.0)
        except Exception:
            # Guard against unexpected hook errors
            pass

    # Prefer single-file mp4 formats to avoid needing ffmpeg merging
    ydl_opts = {
        'format': 'best[ext=mp4]/best',
        'outtmpl': os.path.join(download_path, 'Shorts_%(title)s_%(id)s.%(ext)s'),
        'progress_hooks': [ydl_progress_hook],
        'quiet': True,
        'no_warnings': True,
        'prefer_ffmpeg': False,
    }

    # Ensure download_path exists
    try:
        os.makedirs(download_path, exist_ok=True)
    except Exception:
        pass

    # Wrap outtmpl to sanitize filename by replacing during postprocessing
    # yt-dlp supports a 'outtmpl' but we'll sanitize title via a postprocessor rename if necessary

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])
        return {"success": True}
    except Exception as e:
        # If it failed due to merging/ffmpeg issues, try a safer format
        errstr = str(e)
        if "ffmpeg" in errstr.lower() or "postprocessor" in errstr.lower():
            # Try a more basic format
            ydl_opts['format'] = 'best[ext=mp4]/best'
            try:
                with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                    ydl.download([url])
                return {"success": True, "note": "Downloaded without merging due to missing ffmpeg"}
            except Exception as e2:
                return {"error": str(e2)}
        return {"error": errstr}


# For quick local testing
if __name__ == "__main__":
    def test_callback(p):
        print(f"Progress: {p}%")

    test_url = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
    print(get_video_info(test_url))
