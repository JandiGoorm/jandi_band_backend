"""Real HTTP/MySQL/Redis/R2 checks with simulated Kakao responses.
Reports exclude credentials and tokens; failed feature requests are not retried.
"""
import json
import base64
import os
import struct
import sys
import time
import uuid
import zlib
from http.cookies import SimpleCookie
from datetime import datetime, timedelta, timezone
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.parse import quote, unquote, urlsplit

import boto3
import pymysql
import requests
from botocore.config import Config

API = "http://app:8080"
CONTROL = "http://app:9092"
BUCKET = os.environ["IMAGE_STORAGE_BUCKET"]
PUBLIC = os.environ["IMAGE_STORAGE_PUBLIC_URL"].rstrip("/")
assert BUCKET == "rhythmeet-dev", "Only development storage is allowed"
SESSION = requests.Session()
SESSION.headers["User-Agent"] = "Rhythmeet-Backend-Image-Verification/1.0"
S3 = boto3.client("s3", endpoint_url=os.environ["IMAGE_STORAGE_ENDPOINT"],
                  aws_access_key_id=os.environ["IMAGE_STORAGE_ACCESS_KEY"],
                  aws_secret_access_key=os.environ["IMAGE_STORAGE_SECRET_KEY"],
                  region_name="auto", config=Config(retries={"max_attempts": 0},
                      s3={"addressing_style": "path"}, request_checksum_calculation="when_required"))
RESULTS = []
REQUESTS = []
RUN = uuid.uuid4().hex[:10]
ACTORS = {}
SCENARIO = "setup"


def check(name, passed, detail=""):
    RESULTS.append({"scenario": SCENARIO, "check": name, "passed": bool(passed), "detail": detail})
    print(("PASS " if passed else "FAIL ") + SCENARIO + ": " + name + ("; " + detail if detail else ""))
    return passed


def sql(statement, args=()):
    with DB.cursor() as cursor:
        cursor.execute(statement, args)
        return cursor.fetchall()


def api(method, path, actor=None, expected=(200,), **kwargs):
    headers = {"Authorization": "Bearer " + ACTORS[actor]["token"]} if actor else {}
    r = SESSION.request(method, API + path, headers=headers, timeout=45, **kwargs)
    REQUESTS.append({"scenario": SCENARIO, "method": method, "path": path.split("?")[0], "status": r.status_code})
    if expected is not None and r.status_code not in expected:
        raise AssertionError(f"{method} {path.split('?')[0]} returned HTTP {r.status_code}, expected {expected}")
    return r


def data(r):
    payload = r.json()
    assert payload.get("success"), "API reported failure"
    return payload.get("data")


def png(color):
    def chunk(kind, payload):
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0)) + \
        chunk(b"IDAT", zlib.compress(b"\0" + bytes(color))) + chunk(b"IEND", b"")


RED, BLUE = png((255, 0, 0)), png((0, 0, 255))


def file(body=RED, name="검증 + %.png", mime="image/png"):
    return (name, body, mime)


def key(url):
    assert url.startswith(PUBLIC + "/"), "Unexpected image origin"
    return unquote(url[len(PUBLIC) + 1:])


def exists(url):
    try:
        S3.head_object(Bucket=BUCKET, Key=key(url))
        return True
    except S3.exceptions.ClientError as exc:
        if exc.response["ResponseMetadata"]["HTTPStatusCode"] == 404:
            return False
        raise


def image(name, url, body=RED):
    r = SESSION.get(url, timeout=30)
    check(name + " public bytes/type", r.status_code == 200 and r.content == body and
          r.headers.get("Content-Type", "").split(";")[0] == "image/png", "HTTP " + str(r.status_code))
    if exists(url):
        stored = S3.get_object(Bucket=BUCKET, Key=key(url))
        check(name + " signed R2 bytes", stored["Body"].read() == body)
    else:
        check(name + " signed R2 bytes", False, "object missing")


def removed(name, url):
    check(name + " object removed", not exists(url))
    r = SESSION.get(url, timeout=30)
    check(name + " public URL removed", r.status_code == 404, "HTTP " + str(r.status_code))


def inventory():
    result = {}
    for page in S3.get_paginator("list_objects_v2").paginate(Bucket=BUCKET):
        for item in page.get("Contents", []):
            result[item["Key"]] = (item["Size"], item["ETag"])
    return result


def login(actor, registered=True):
    r = api("GET", "/api/auth/login", params={"code": "image-test-" + RUN + "-" + actor})
    d = data(r)
    check(actor + " login registration state", d["isRegistered"] == registered)
    check(actor + " access header/cookie", bool(r.headers.get("AccessToken")) and
          "HttpOnly" in r.headers.get("Set-Cookie", "") and "Secure" in r.headers.get("Set-Cookie", ""))
    cookie = SimpleCookie()
    cookie.load(r.headers["Set-Cookie"])
    ACTORS[actor] = {"token": r.headers["AccessToken"], "refresh": cookie["RefreshToken"].value}


def register(actor):
    login(actor, False)
    d = data(api("POST", "/api/auth/signup", actor, json={"position": "GUITAR", "university": "Image Test University"}))
    ACTORS[actor]["id"] = d["id"]
    check(actor + " signup profile", d["profilePhoto"] == "https://example.invalid/kakao-profile.png")
    uid = d["id"]
    login(actor)
    ACTORS[actor]["id"] = uid
    check(actor + " existing account reused", data(api("GET", "/api/users/me/info", actor))["id"] == uid)


def auth_profile():
    register("owner")
    register("other")
    register("admin")
    sql("UPDATE users SET admin_role='ADMIN' WHERE user_id=%s", (ACTORS["admin"]["id"],))
    login("admin")
    check("duplicate signup rejected", api("POST", "/api/auth/signup", "owner", expected=None,
          json={"position": "GUITAR", "university": "Image Test University"}).status_code in (400, 403, 409))
    check("invalid provider code rejected", api("GET", "/api/auth/login", expected=None,
          params={"code": "invalid-test-code"}).status_code in (400, 401))
    r = api("POST", "/api/auth/refresh", json={"refreshToken": ACTORS["owner"]["refresh"]})
    check("API refresh emits usable access token", bool(r.headers.get("AccessToken")))
    ACTORS["owner"]["token"] = r.headers["AccessToken"]
    p = "/api/users/me/info"
    first = data(api("PATCH", p, "owner", files={"profilePhoto": file()}))["profilePhoto"]
    image("profile initial", first)
    check("profile persisted", data(api("GET", p, "owner"))["profilePhoto"] == first)
    second = data(api("PATCH", p, "owner", files={"profilePhoto": file(BLUE)}))["profilePhoto"]
    image("profile replacement", second, BLUE)
    removed("profile old", first)
    unchanged = data(api("PATCH", p, "owner", data={"nickname": "Image test owner"}))["profilePhoto"]
    check("profile metadata keeps URL", unchanged == second)
    image("profile metadata keeps file", second, BLUE)
    empty = data(api("PATCH", p, "owner", files={"profilePhoto": file(b"")}))["profilePhoto"]
    check("empty profile upload ignored", empty == second)


def new_club(name="Flow club"):
    return data(api("POST", "/api/clubs", "owner", expected=(201,), json={"name": name + " " + RUN}))


def club_gallery():
    c = new_club()
    p = "/api/clubs/" + str(c["id"])
    default = c["photoUrl"]
    check("new club uses R2 default", default == PUBLIC + "/club-photo/rhythmeet.webp" and exists(default))
    first = data(api("POST", p + "/main-image", "owner", files={"image": file()}))
    image("club main", first)
    second = data(api("POST", p + "/main-image", "owner", files={"image": file(BLUE)}))
    image("club replacement", second, BLUE)
    removed("club previous", first)
    for method in ("POST", "DELETE"):
        kwargs = {"files": {"image": file()}} if method == "POST" else {}
        r = api(method, p + "/main-image", "other", expected=None, **kwargs)
        check("outsider cannot " + method + " club image", r.status_code in (401, 403), "HTTP " + str(r.status_code))
    check("denied club edit preserves image", data(api("GET", p))["photoUrl"] == second and exists(second))
    api("DELETE", p + "/main-image", "owner")
    removed("club reset", second)
    check("club reset default remains", data(api("GET", p))["photoUrl"] == default and exists(default))
    g = data(api("POST", p + "/photo", "owner", files={"image": file()}, data={"description": "before", "isPublic": "true"}))
    gp = p + "/photo/" + str(g["photoId"])
    image("gallery created", g["imageUrl"])
    check("public gallery visible to outsider", data(api("GET", gp, "other"))["imageUrl"] == g["imageUrl"])
    updated = data(api("PATCH", gp, "owner", data={"description": "changed", "isPublic": "false"}))
    check("gallery metadata URL unchanged", updated["imageUrl"] == g["imageUrl"])
    image("gallery metadata retains file", g["imageUrl"])
    check("private gallery detail denied", api("GET", gp, "other", expected=None).status_code == 403)
    listing = data(api("GET", p + "/photo", "other"))
    check("private gallery omitted from outsider list", str(g["photoId"]) not in [str(v["photoId"]) for v in listing["content"]])
    for method in ("PATCH", "DELETE"):
        r = api(method, gp, "other", expected=None, data={"description": "forbidden"} if method == "PATCH" else None)
        check("outsider cannot " + method + " gallery", r.status_code == 403, "HTTP " + str(r.status_code))
    rep = data(api("PATCH", gp, "owner", files={"image": file(BLUE)}, data={"isPublic": "true"}))
    image("gallery replacement", rep["imageUrl"], BLUE)
    removed("gallery replaced file", g["imageUrl"])
    api("PATCH", gp + "/pin", "owner")
    check("gallery pin persisted", data(api("GET", gp, "owner"))["isPinned"] is True)
    api("PATCH", gp + "/pin", "owner")
    check("gallery unpin persisted", data(api("GET", gp, "owner"))["isPinned"] is False)
    api("DELETE", gp, "owner")
    removed("gallery deleted", rep["imageUrl"])
    check("deleted gallery detail 404", api("GET", gp, "owner", expected=None).status_code == 404)
    main = data(api("POST", p + "/main-image", "owner", files={"image": file()}))
    gal = data(api("POST", p + "/photo", "owner", files={"image": file()}))["imageUrl"]
    api("DELETE", p, "owner")
    removed("club cascade main", main)
    removed("club cascade gallery", gal)
    check("club cascade preserves default", exists(default))


def promo():
    fields = {"teamName": "Image tests", "title": "Image promo " + RUN,
              "admissionFee": "0", "eventDatetime": "2027-01-10T12:00:00", "location": "test"}
    d = data(api("POST", "/api/promos", "owner", data=fields, files={"image": file()}))
    p = "/api/promos/" + str(d["id"])
    first = data(api("GET", p))["photoUrls"][0]
    image("promo create", first)
    check("promo list links image", first in json.dumps(data(api("GET", "/api/promos"))))
    for method in ("PATCH", "DELETE"):
        kwargs = {"files": {"image": file(BLUE)}} if method == "PATCH" else {}
        r = api(method, p, "other", expected=None, **kwargs)
        check("other user cannot " + method + " promo", r.status_code == 403, "HTTP " + str(r.status_code))
    api("PATCH", p, "owner", files={"title": (None, "Updated title")})
    check("promo text edit retains image", data(api("GET", p))["photoUrls"] == [first] and exists(first))
    api("PATCH", p, "owner", files={"image": file(BLUE)})
    second = data(api("GET", p))["photoUrls"][0]
    image("promo replacement", second, BLUE)
    removed("promo previous", first)
    api("PATCH", p, "owner", files={"deleteImageUrl": (None, second)})
    check("promo remove clears API reference", data(api("GET", p))["photoUrls"] == [])
    removed("promo remove", second)
    reattach = api("PATCH", p, "owner", expected=None, files={"image": file()})
    check("promo reattach after removal succeeds", reattach.status_code == 200, "HTTP " + str(reattach.status_code))
    third_urls = data(api("GET", p))["photoUrls"]
    if reattach.ok:
        image("promo reattach", third_urls[0])
    api("POST", p + "/like", "other")
    check("promo like keeps image references", data(api("GET", p, "other"))["isLikedByUser"] is True and
          data(api("GET", p))["photoUrls"] == third_urls)
    api("DELETE", p, "owner")
    for url in third_urls:
        removed("promo delete", url)
    check("deleted promo 404", api("GET", p, expected=None).status_code == 404)
    fresh = data(api("POST", "/api/promos", "owner", data=fields, files={"image": file()}))["id"]
    fresh_path = "/api/promos/" + str(fresh)
    attached = data(api("GET", fresh_path))["photoUrls"][0]
    api("DELETE", fresh_path, "owner")
    removed("promo delete with attached image", attached)


def gallery_members():
    c = new_club("Members club")
    p = "/api/clubs/" + str(c["id"])
    invite = data(api("POST", "/api/invite/clubs/" + str(c["id"]), "owner"))["code"]
    data(api("POST", "/api/join/clubs", "other", params={"code": invite}))
    g = data(api("POST", p + "/photo", "other", files={"image": file()}, data={"isPublic": "false"}))
    gp = p + "/photo/" + str(g["photoId"])
    image("member uploads private gallery", g["imageUrl"])
    check("representative reads member private photo", data(api("GET", gp, "owner"))["imageUrl"] == g["imageUrl"])
    check("member cannot pin gallery", api("PATCH", gp + "/pin", "other", expected=None).status_code == 403)
    api("PATCH", gp + "/pin", "owner")
    check("representative can pin member photo", data(api("GET", gp, "owner"))["isPinned"] is True)
    check("representative cannot replace member photo", api("PATCH", gp, "owner", expected=None,
          files={"image": file(BLUE)}).status_code == 403)
    api("DELETE", gp, "owner")
    removed("representative removes member photo", g["imageUrl"])
    main = data(api("POST", p + "/main-image", "other", files={"image": file()}))
    image("member may change club main image", main)
    api("DELETE", p, "owner")
    removed("member main image cascaded", main)


def notice_fields():
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    return {"title": "Image notice " + RUN, "content": "Backend API verification",
            "startDatetime": (now - timedelta(days=1)).isoformat(timespec="seconds"),
            "endDatetime": (now + timedelta(days=1)).isoformat(timespec="seconds")}


def notice():
    fields = notice_fields()
    check("regular user cannot create notice", api("POST", "/api/notices", "owner", expected=None,
          data=fields, files={"image": file()}).status_code == 403)
    n = data(api("POST", "/api/notices", "admin", expected=(201,), data=fields, files={"image": file()}))
    p = "/api/notices/" + str(n["id"])
    first = n["imageUrl"]
    image("notice create", first)
    check("active notices include image", first in json.dumps(data(api("GET", "/api/notices/active", "owner"))))
    meta = data(api("PATCH", p, "admin", data={"content": "Changed text"}))
    check("notice text edit preserves image", meta["imageUrl"] == first and exists(first))
    api("PATCH", p + "/toggle-pause", "admin")
    check("paused notice hidden", first not in json.dumps(data(api("GET", "/api/notices/active", "owner"))))
    api("PATCH", p + "/toggle-pause", "admin")
    second = data(api("PATCH", p, "admin", files={"image": file(BLUE)}))["imageUrl"]
    image("notice replacement", second, BLUE)
    removed("notice previous", first)
    result = data(api("PATCH", p, "admin", data={"deleteImage": "true"}))
    check("notice image reference cleared", result["imageUrl"] is None)
    removed("notice image removal", second)
    third = data(api("PATCH", p, "admin", files={"image": file()}))["imageUrl"]
    for method in ("PATCH", "DELETE"):
        kwargs = {"files": {"image": file(BLUE)}} if method == "PATCH" else {}
        check("regular user cannot " + method + " notice", api(method, p, "owner", expected=None, **kwargs).status_code == 403)
    api("DELETE", p, "admin")
    removed("notice delete", third)
    check("deleted notice 404", api("GET", p, "admin", expected=None).status_code == 404)


def admin_storage():
    fields = {"dirName": "api-test/한글 space+percent%"}
    r = api("POST", "/api/images/upload", "owner", expected=None, data=fields, files={"file": file()})
    check("ordinary user upload returns forbidden", r.status_code == 403, "HTTP " + str(r.status_code))
    url = data(api("POST", "/api/images/upload", "admin", data=fields, files={"file": file()}))
    image("encoded key upload", url)
    check("special characters encoded", "%2B" in url and "%25" in url and "%20" in url)
    for target in [url + "?x=1", url + "#fragment", url.replace("https://", "https://evil.example/"),
                   PUBLIC + "/club-photo/rhythmeet.webp"]:
        api("DELETE", "/api/images", "admin", params={"fileUrl": target})
        check("unmanaged/default deletion does not delete test file", exists(url))
    api("DELETE", "/api/images", "admin", params={"fileUrl": "https://retired-storage.example/" + quote(key(url), safe="/")})
    check("retired storage URL cannot delete R2 file", exists(url))
    api("DELETE", "/api/images", "admin", params={"fileUrl": url})
    removed("current public URL deletes active bucket object", url)
    for directory in ["../bad", "safe/../bad", "/absolute", "bad\\path", "double//slash"]:
        r = api("POST", "/api/images/upload", "admin", expected=None,
                data={"dirName": directory}, files={"file": file()})
        check("reject invalid storage directory " + directory, r.status_code == 400, "HTTP " + str(r.status_code))
    r = api("POST", "/api/images/upload", "admin", expected=None,
            data={"dirName": "api-test"}, files={"file": file(name="noextension")})
    check("reject extensionless file", r.status_code == 400)


def invalid_uploads():
    c = new_club("Validation club")
    cp = "/api/clubs/" + str(c["id"])
    routes = [
        ("profile", "PATCH", "/api/users/me/info", "owner", "profilePhoto", {}),
        ("club", "POST", cp + "/main-image", "owner", "image", {}),
        ("gallery", "POST", cp + "/photo", "owner", "image", {}),
        ("promo", "POST", "/api/promos", "owner", "image", {"teamName": "validation", "title": "validation"}),
        ("notice", "POST", "/api/notices", "admin", "image", notice_fields()),
        ("admin", "POST", "/api/images/upload", "admin", "file", {"dirName": "api-test"}),
    ]
    for name, method, path, actor, field, fields in routes:
        before = len(SESSION.get(CONTROL + "/fixture/status", timeout=5).json()["keys"])
        r = api(method, path, actor, expected=None, data=fields, files={field: file(b"not an image", "fixture.txt", "text/plain")})
        check(name + " rejects non-image file", r.status_code in (400, 415, 422), "HTTP " + str(r.status_code))
        after = len(SESSION.get(CONTROL + "/fixture/status", timeout=5).json()["keys"])
        check(name + " invalid upload makes no storage write", before == after)
    for name, method, path, _, field, fields in routes:
        r = api(method, path, expected=None, data=fields, files={field: file()})
        check(name + " anonymous upload returns 401", r.status_code == 401, "HTTP " + str(r.status_code))
    r = api("POST", cp + "/photo", "owner", expected=None, files={"image": file(b"")})
    check("gallery rejects empty file", r.status_code in (400, 422), "HTTP " + str(r.status_code))
    r = api("POST", "/api/images/upload", "admin", expected=None, data={"dirName": "api-test"},
            files={"file": file(b"x" * (10 * 1024 * 1024 + 1))})
    check("oversized upload rejected", r.status_code in (400, 413), "HTTP " + str(r.status_code))


def failure_cases():
    profile = "/api/users/me/info"
    url = data(api("PATCH", profile, "owner", files={"profilePhoto": file()}))["profilePhoto"]
    yield "profile", "PATCH", profile, "owner", "profilePhoto", url, "user_photo", "profilePhoto"
    c = new_club("Failure club")
    cp = "/api/clubs/" + str(c["id"])
    url = data(api("POST", cp + "/main-image", "owner", files={"image": file()}))
    yield "club", "POST", cp + "/main-image", "owner", "image", url, "club_photo", "photoUrl"
    g = data(api("POST", cp + "/photo", "owner", files={"image": file()}))
    yield "gallery", "PATCH", cp + "/photo/" + str(g["photoId"]), "owner", "image", g["imageUrl"], "club_gal_photo", "imageUrl"
    n = data(api("POST", "/api/notices", "admin", expected=(201,), data=notice_fields(), files={"image": file()}))
    yield "notice", "PATCH", "/api/notices/" + str(n["id"]), "admin", "image", n["imageUrl"], "notice", "imageUrl"
    p = data(api("POST", "/api/promos", "owner", data={"teamName": "failure", "title": "failure"}, files={"image": file()}))
    pp = "/api/promos/" + str(p["id"])
    url = data(api("GET", pp))["photoUrls"][0]
    yield "promo", "PATCH", pp, "owner", "image", url, "promo_photo", "photoUrls"


def image_failures(database):
    for name, method, path, actor, field, original, table, ref in failure_cases():
        before = set(SESSION.get(CONTROL + "/fixture/status", timeout=5).json()["keys"])
        if database:
            assert table in {"user_photo", "club_photo", "club_gal_photo", "notice", "promo_photo"}
            sql("CREATE TRIGGER image_test_fail_update BEFORE UPDATE ON " + table +
                " FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Injected image test DB failure'")
        else:
            SESSION.post(CONTROL + "/fixture/fail-next-put", timeout=5).raise_for_status()
        try:
            r = api(method, path, actor, expected=None, files={field: file(BLUE)})
            check(name + " failure returned to caller", not r.ok, "HTTP " + str(r.status_code))
        finally:
            if database:
                sql("DROP TRIGGER image_test_fail_update")
        get_path = path.removesuffix("/main-image")
        saved = data(api("GET", get_path, actor))[ref]
        check(name + " rollback retains DB reference", saved == original or saved == [original])
        check(name + " rollback preserves original R2 object", exists(original))
        attempted = set(SESSION.get(CONTROL + "/fixture/status", timeout=5).json()["keys"]) - before
        check(name + " failure leaves no new R2 object", all(not exists(PUBLIC + "/" + quote(k, safe="/")) for k in attempted))


def upload_failure():
    image_failures(False)


def db_failure():
    image_failures(True)


def concurrent_updates():
    for name, method, path, actor, field, original, table, ref in failure_cases():
        before = set(SESSION.get(CONTROL + "/fixture/status", timeout=5).json()["keys"])
        def upload(body):
            return requests.request(method, API + path, headers={"Authorization": "Bearer " + ACTORS[actor]["token"]},
                                    files={field: file(body)}, timeout=60)
        with ThreadPoolExecutor(max_workers=2) as pool:
            responses = list(pool.map(upload, [RED, BLUE]))
        for response in responses:
            REQUESTS.append({"scenario": SCENARIO, "method": method, "path": path, "status": response.status_code})
        check(name + " concurrent uploads succeed", all(r.status_code == 200 for r in responses))
        saved = data(api("GET", path.removesuffix("/main-image"), actor))[ref]
        current = saved[0] if isinstance(saved, list) else saved
        created = set(SESSION.get(CONTROL + "/fixture/status", timeout=5).json()["keys"]) - before
        remaining = [k for k in created if exists(PUBLIC + "/" + quote(k, safe="/"))]
        check(name + " concurrent updates leave one referenced object", len(created) == 2 and remaining == [key(current)])
        check(name + " concurrent updates remove original", not exists(original))
        if name in ("gallery", "notice"):
            suffix = "/pin" if name == "gallery" else "/toggle-pause"
            flag = "isPinned" if name == "gallery" else "isPaused"
            previous_flag = data(api("GET", path, actor))[flag]
            with ThreadPoolExecutor(max_workers=2) as pool:
                future = pool.submit(upload, BLUE)
                api("PATCH", path + suffix, actor)
                response = future.result()
            REQUESTS.append({"scenario": SCENARIO, "method": method, "path": path, "status": response.status_code})
            check(name + " upload concurrent with toggle succeeds", response.status_code == 200)
            saved = data(api("GET", path, actor))
            check(name + " concurrent toggle and image both persist", saved[flag] != previous_flag and saved[ref] != current)
            image(name + " image survives concurrent toggle", saved[ref], BLUE)
            removed(name + " concurrent toggle old object deleted", current)


def delete_failure():
    p = "/api/users/me/info"
    old = data(api("PATCH", p, "owner", files={"profilePhoto": file()}))["profilePhoto"]
    SESSION.post(CONTROL + "/fixture/fail-next-delete", timeout=5).raise_for_status()
    r = api("PATCH", p, "owner", expected=None, files={"profilePhoto": file(BLUE)})
    check("post-commit cleanup failure is explicit", r.status_code == 502 and r.json().get("errorCode") == "IMAGE_CLEANUP_FAILED")
    current = data(api("GET", p, "owner"))["profilePhoto"]
    image("committed image survives cleanup error", current, BLUE)
    check("failed deletion still has original object", exists(old))
    api("DELETE", "/api/images", "admin", params={"fileUrl": old})
    removed("explicit administrator cleanup", old)


def file_validation():
    fields = {"dirName": "api-test/formats"}
    for name, content, mime in [
        ("fake.png", b"not an image", "image/png"),
        ("wrong.jpg", RED, "image/jpeg"),
        ("wrong-mime.png", RED, "image/jpeg"),
        ("truncated.png", RED[:8], "image/png"),
    ]:
        r = api("POST", "/api/images/upload", "admin", expected=None, data=fields,
                files={"file": file(content, name, mime)})
        check("reject forged/mismatched image " + name, r.status_code == 400, "HTTP " + str(r.status_code))
    webp = base64.b64decode("UklGRhwAAABXRUJQVlA4TA8AAAAvAAAAAAcQ/Y/+ByKi/wEA")
    url = data(api("POST", "/api/images/upload", "admin", data=fields, files={"file": file(webp, "tiny.WEBP", "image/webp")}))
    response = SESSION.get(url, timeout=30)
    check("real uppercase WebP round trip", response.status_code == 200 and response.content == webp and
          response.headers.get("Content-Type") == "image/webp")
    api("DELETE", "/api/images", "admin", params={"fileUrl": url})
    removed("WebP deleted", url)


def withdrawal():
    register("withdraw")
    url = data(api("PATCH", "/api/users/me/info", "withdraw", files={"profilePhoto": file()}))["profilePhoto"]
    uid = ACTORS["withdraw"]["id"]
    api("POST", "/api/auth/cancel", "withdraw")
    check("withdrawal soft-deletes account", sql("SELECT deleted_at IS NOT NULL FROM users WHERE user_id=%s", (uid,))[0][0] == 1)
    check("withdrawal soft-deletes profile row", sql("SELECT deleted_at IS NOT NULL FROM user_photo WHERE user_id=%s", (uid,))[0][0] == 1)
    check("withdrawal retention keeps image until hard-delete", exists(url))
    r = api("GET", "/api/auth/login", expected=None, params={"code": "image-test-" + RUN + "-withdraw"})
    check("withdrawn account cannot log in", r.status_code == 403)


def main():
    global DB, SCENARIO
    for _ in range(90):
        try:
            if SESSION.get(API + "/api/clubs", timeout=2).status_code == 200:
                break
        except requests.RequestException:
            pass
        time.sleep(1)
    else:
        raise RuntimeError("Test app did not become ready")
    DB = pymysql.connect(host="image-api-mysql", user="image_test", password="image-test-only",
                         database="image_api_test", autocommit=True)
    assert sql("SELECT COUNT(*) FROM users")[0][0] == 0, "Refuse to reuse a populated database"
    sql("INSERT INTO region(code,name) VALUES('IMAGE_TEST','Image Test Region')")
    sql("INSERT INTO university(university_code,name,region_id) VALUES('IMGTEST','Image Test University',(SELECT region_id FROM region WHERE code='IMAGE_TEST'))")
    before = inventory()
    try:
        for scenario in [auth_profile, club_gallery, gallery_members, promo, notice, admin_storage,
                         invalid_uploads, file_validation, upload_failure, db_failure,
                         concurrent_updates, delete_failure, withdrawal]:
            SCENARIO = scenario.__name__
            print("SCENARIO " + SCENARIO)
            try:
                scenario()
            except Exception as exc:
                # Failure context without response bodies, cookies or credential-bearing exception text.
                detail = str(exc) if isinstance(exc, AssertionError) else type(exc).__name__
                check("scenario completed", False, detail)
    finally:
        SCENARIO = "cleanup"
        state = SESSION.get(CONTROL + "/fixture/status", timeout=5).json()
        owned = set(state["keys"])
        assert not owned.intersection(before), "A test upload collided with baseline data"
        for k in owned:
            S3.delete_object(Bucket=BUCKET, Key=k)
        after = inventory()
        check("all attempted test objects removed", not owned.intersection(after))
        check("baseline keys/sizes/ETags unchanged", all(after.get(k) == v for k, v in before.items()))
        check("bucket inventory exactly restored", after == before)
        check("Kakao HTTP provider was exercised", state["tokenCalls"] >= 7 and state["userCalls"] >= 7)
        DB.close()
        report = {"run": RUN, "utc": datetime.now(timezone.utc).isoformat(),
                  "environment": "isolated MySQL 8.4 + Redis; real development R2; simulated Kakao responses",
                  "baselineObjects": len(before), "testObjectKeys": len(owned),
                  "passed": sum(r["passed"] for r in RESULTS), "failed": sum(not r["passed"] for r in RESULTS),
                  "checks": RESULTS, "requests": REQUESTS}
        Path("/reports").mkdir(parents=True, exist_ok=True)
        Path("/reports/result-" + RUN + ".json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print("SUMMARY " + json.dumps({k: report[k] for k in ["passed", "failed", "baselineObjects", "testObjectKeys"]}))
    return 1 if any(not r["passed"] for r in RESULTS) else 0


if __name__ == "__main__":
    sys.exit(main())
