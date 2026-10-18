"""Host regression implementation of the Android OnePager algorithm (OpenCV).
PDF geometry is in assets/onepager_profile.json. No OCR; no network requests.
"""
from pathlib import Path
import json
import cv2
import numpy as np

ASSETS = Path(__file__).resolve().parents[1] / 'app/src/main/assets'
PROFILE = json.loads((ASSETS / 'onepager_profile.json').read_text())


def analyze(image):
    template = cv2.imread(str(ASSETS / 'onepager_template.png'), cv2.IMREAD_GRAYSCALE)
    photo = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY) if image.ndim == 3 else image
    orb = cv2.ORB_create(4500)
    rk, rd = orb.detectAndCompute(template, None)
    pk, pd = orb.detectAndCompute(photo, None)
    if pd is None:
        raise ValueError('No printed features')
    pairs = cv2.BFMatcher(cv2.NORM_HAMMING).knnMatch(rd, pd, k=2)
    good = [p[0] for p in pairs if len(p) == 2 and p[0].distance < .72*p[1].distance]
    if len(good) < 30:
        raise ValueError('Not enough visual matches')
    h, mask = cv2.findHomography(np.float32([pk[m.trainIdx].pt for m in good]),
                                 np.float32([rk[m.queryIdx].pt for m in good]), cv2.RANSAC, 4)
    if h is None or mask.sum() < 30 or mask.mean() < .25:
        raise ValueError('Uncertain registration')
    size = (template.shape[1], template.shape[0])
    coarse = cv2.warpPerspective(photo, h, size, borderValue=255)

    def match_shift(image, rect, sx, sy):
        x, y, width, height = rect
        patch = template[y:y+height, x:x+width]
        search = image[y-sy:y+height+sy, x-sx:x+width+sx]
        _, score, _, point = cv2.minMaxLoc(cv2.matchTemplate(search, patch, cv2.TM_CCOEFF_NORMED))
        return np.array(point)-np.array([sx, sy]), score

    markers = np.float32(PROFILE['markers'])*3
    found, scores = [], []
    for r, target in zip(PROFILE['markerRects'], markers):
        x0, y0, x1, y1 = np.round(np.array(r)*3).astype(int)
        shift, score = match_shift(coarse, (x0-12, y0-12, x1-x0+24, y1-y0+24), 24, 24)
        if score < .65:
            raise ValueError('Missing L mark')
        found.append(target+shift)
        scores.append(score)
    correction = cv2.getPerspectiveTransform(np.float32(found), markers)
    aligned = cv2.warpPerspective(photo, correction @ h, size, borderValue=255)
    binary = cv2.adaptiveThreshold(aligned, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                  cv2.THRESH_BINARY_INV, 41, 12)
    overlay = cv2.cvtColor(aligned, cv2.COLOR_GRAY2BGR)

    def fill(x, y, radius):
        return np.mean([binary[y+dy, x+dx] > 0 for dy in range(-radius, radius+1)
                        for dx in range(-radius, radius+1) if dx*dx+dy*dy <= radius*radius])
    if fill(round(20.8356*3), round(20.726*3), 3) <= .45:
        raise ValueError('Missing top-left orientation dot')
    values = [[None]*7 for _ in range(4)]
    frame_scores = []
    for d, row in enumerate(PROFILE['sliders']):
        for m, rect in enumerate(row):
            x0, y0, x1, y1 = np.round(np.array(rect)*3).astype(int)
            (dx, dy), score = match_shift(aligned, (x0, y0-3, x1-x0, y1-y0+33), 12, 18)
            frame_scores.append(score)
            if score < .45:
                continue
            left, top = x0+dx+4, y0+dy+5
            roi = binary[top:y1+dy-5, left:x1+dx-4]
            n, labels, stats, centers = cv2.connectedComponentsWithStats(roi)
            candidates = []
            for i in range(1, n):
                x, y, width, height, area = stats[i]
                if area >= 12 and area <= roi.size*.3 and 5 <= width <= 36 and height >= 5 and .35 <= width/height <= 2.8:
                    candidates.append(i)
            if len(candidates) == 1:
                point = centers[candidates[0]]+np.array([left, top])
                value = float(np.clip(10*(point[0]-(x0+dx))/(x1-x0), 0, 10))
                values[m][d] = round(value, 2)
                cv2.circle(overlay, tuple(np.round(point).astype(int)), 12, (70, 145, 30), 2)
                cv2.putText(overlay, f'{value:.1f}', (int(x0+dx+5), int(y0+dy-5)), cv2.FONT_HERSHEY_SIMPLEX, .45, (70, 115, 25), 1)
            elif len(candidates) > 1:
                for i in candidates:
                    point = centers[i]+np.array([left, top])
                    cv2.circle(overlay, tuple(np.round(point).astype(int)), 12, (0, 130, 225), 2)
    if sum(s >= .45 for s in frame_scores) < 22:
        raise ValueError('Wrong slider layout')
    factors = []
    for d, x in enumerate(PROFILE['factorX']):
        bits = 0
        for f, y in enumerate(PROFILE['factorY']):
            cx, cy = round(x*3), round(y*3)
            if fill(cx, cy, 7) >= .5:
                bits |= 1 << f
                cv2.circle(overlay, (cx, cy), 15, (70, 145, 30), 2)
        factors.append(bits)
    return {'values': values, 'factors': factors, 'markerScores': scores, 'frameScores': frame_scores}, overlay


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('image')
    parser.add_argument('--overlay', default='onepager-recognized.jpg')
    args = parser.parse_args()
    result, overlay = analyze(cv2.imread(args.image))
    cv2.imwrite(args.overlay, overlay)
    print(json.dumps(result, indent=2))
