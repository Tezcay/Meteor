"""Check frozen demo ZIP receipts against preview images and V13; never rewrite a migration."""
from pathlib import Path
import hashlib
import json
import zipfile

root = Path(__file__).resolve().parent.parent
pack_root = root / "backend/src/main/resources/delivery-fixtures/showcase"
catalog = json.loads((pack_root / "catalog.json").read_text(encoding="utf-8"))
migration = (root / "backend/src/main/resources/db/migration/V13__import_showcase_delivery_products.sql").read_text(encoding="utf-8")
count = 0
for item in catalog:
    path = pack_root / (item["slug"] + ".zip")
    payload = path.read_bytes()
    assert len(payload) == item["sizeBytes"], path
    digest = hashlib.sha256(payload).hexdigest()
    assert digest == item["sha256"] and digest in migration, path
    assert f"'application/zip', {len(payload)}, '{digest}'" in migration, path
    with zipfile.ZipFile(path) as archive:
        expected = ["README.txt"] + [Path(image).name for image in item["images"]]
        assert archive.namelist() == expected, path
        for image in item["images"]:
            assert archive.read(Path(image).name) == (root / "frontend/public/images" / image).read_bytes(), image
            count += 1
print(f"Verified {len(catalog)} frozen ZIP files, {count} exact preview-image receipts and V13 hashes.")
