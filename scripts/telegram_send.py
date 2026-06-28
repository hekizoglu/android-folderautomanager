import os, sys, urllib.request, urllib.parse
msg = sys.argv[1] if len(sys.argv) > 1 else "Test"
# msg contains \n - encode properly
from dotenv import load_dotenv
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))
token = os.getenv('TELEGRAM_BOT_TOKEN')
chat_id = os.getenv('TELEGRAM_CHAT_ID')
url = f"https://api.telegram.org/bot{token}/sendMessage"
data = urllib.parse.urlencode({'chat_id': chat_id, 'text': msg}).encode()
req = urllib.request.Request(url, data=data)
urllib.request.urlopen(req, timeout=10).read()
print("OK")
