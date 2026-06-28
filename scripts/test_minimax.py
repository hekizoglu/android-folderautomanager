import os, json, urllib.request
from dotenv import load_dotenv
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))
key = os.getenv('MINIMAX_API_KEY')
url = 'https://api.minimax.io/v1/text/chatcompletion_v2'
data = json.dumps({'model':'MiniMax-M2.7','messages':[{'role':'user','content':'1+1=?'}],'max_tokens':30}).encode()
req = urllib.request.Request(url, data=data, headers={'Authorization':f'Bearer {key}','Content-Type':'application/json'})
r = json.loads(urllib.request.urlopen(req, timeout=15).read())
print(json.dumps(r, indent=2)[:500])
