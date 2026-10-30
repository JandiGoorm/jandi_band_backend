"""Local-only Kakao HTTP response fixture; not part of the production image."""
import json
import os
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def respond(self, status, body):
        payload = json.dumps(body).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def do_POST(self):
        fields = parse_qs(self.rfile.read(int(self.headers.get('Content-Length', '0'))).decode())
        if self.path == '/oauth/token':
            code = fields.get('code', [''])[0]
            if not re.fullmatch(r'image-test-browser-[a-z0-9-]+', code):
                return self.respond(400, {'error': 'invalid_grant'})
            return self.respond(200, {'access_token': code, 'refresh_token': 'local-only', 'expires_in': 3600})
        if self.path == '/v1/user/unlink':
            return self.respond(200, {'id': fields.get('target_id', [''])[0]})
        self.respond(404, {})

    def do_GET(self):
        identifier = self.headers.get('Authorization', '').removeprefix('Bearer ')
        if self.path == '/v2/user/me' and re.fullmatch(r'image-test-browser-[a-z0-9-]+', identifier):
            avatar = os.environ['IMAGE_STORAGE_PUBLIC_URL'].rstrip('/') + '/club-photo/rhythmeet.webp'
            return self.respond(200, {'id': identifier, 'kakao_account': {'profile': {
                'nickname': '로컬 개발 사용자', 'profile_image_url': avatar}}})
        self.respond(404, {})


if __name__ == '__main__':
    ThreadingHTTPServer(('127.0.0.1', 9092), Handler).serve_forever()
