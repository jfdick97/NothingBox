"""Host-side regression of the same marker geometry and fill rules as Scanner.kt.
Requires Python, numpy and opencv-python-headless. No Android device needed.
"""
import json
from pathlib import Path
import cv2
import numpy as np

ASSETS = Path(__file__).resolve().parents[1] / 'app/src/main/assets'
PROFILE = json.loads((ASSETS / 'profile.json').read_text())


def recognize(image):
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    corners, ids, _ = cv2.aruco.ArucoDetector(
        cv2.aruco.getPredefinedDictionary(cv2.aruco.DICT_4X4_50)).detectMarkers(gray)
    if ids is None or not set(range(4)).issubset(ids.flatten()):
        raise ValueError('Missing markers')
    found = {int(i): c.mean(axis=1)[0] for i, c in zip(ids.flatten(), corners)}
    source = np.float32([found[i] for i in range(4)])
    target = np.float32(PROFILE['markers']) * 3
    h = cv2.getPerspectiveTransform(source, target)
    gray = cv2.warpPerspective(gray, h, (round(PROFILE['width']*3), round(PROFILE['height']*3)), borderValue=255)
    visible = 0
    for grid in PROFILE['grids']:
        for x in grid['x']:
            for v in range(11):
                y = grid['yZero']+(grid['yTen']-grid['yZero'])*v/10
                visible += sum(gray[round(y*3+7.5*np.sin(a*np.pi/6)),
                                    round(x*3+7.5*np.cos(a*np.pi/6))] < 210 for a in range(12)) >= 2
    if visible < 190:
        raise ValueError('Wrong layout or blur')
    binary = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY_INV, 41, 12)

    def fill(x, y):
        cx, cy = round(x*3), round(y*3)
        return np.mean([binary[cy+dy, cx+dx] > 0 for dy in range(-4, 5)
                        for dx in range(-4, 5) if dx*dx+dy*dy <= 16])

    values = []
    for grid in PROFILE['grids']:
        row = []
        for x in grid['x']:
            scores = [fill(x, grid['yZero']+(grid['yTen']-grid['yZero'])*v/10) for v in range(11)]
            marked = [v for v, s in enumerate(scores) if s >= .48]
            weak = any(.23 <= s < .48 for s in scores)
            row.append(marked[0] if len(marked) == 1 and not weak else None)
        values.append(row)
    factors = [sum(1 << f for f, y in enumerate(PROFILE['factors']['y']) if fill(x, y) >= .48)
               for x in PROFILE['factors']['x']]
    return {'values': values, 'factors': factors}


def main():
    expected = json.loads((ASSETS / 'sample_expected.json').read_text())
    sample = cv2.imread(str(ASSETS / 'sample_scan.jpg'))
    for label, image in [('perspective', sample), ('rotated', cv2.rotate(sample, cv2.ROTATE_90_CLOCKWISE)),
                         ('mild blur', cv2.GaussianBlur(sample, (3, 3), .6))]:
        actual = recognize(image)
        assert actual == expected, (label, actual, expected)
        print(f'PASS {label}: 28 values + 7 factor masks')
    blank = cv2.imread(str(ASSETS / 'template.png'))
    assert recognize(blank) == {'values': [[None]*7 for _ in range(4)], 'factors': [0]*7}
    print('PASS blank: missing stays missing')
    grid = PROFILE['grids'][0]
    for v in (3, 8):
        cv2.circle(blank, (round(grid['x'][0]*3), round((grid['yZero']+(grid['yTen']-grid['yZero'])*v/10)*3)), 7, (0, 0, 0), -1)
    assert recognize(blank)['values'][0][0] is None
    print('PASS ambiguous double mark: rejected')
    try:
        recognize(np.full_like(sample, 255))
    except ValueError:
        print('PASS missing markers: rejected')
    else:
        raise AssertionError('Missing markers accepted')
    wrong = cv2.imread(str(ASSETS / 'template.png'))
    wrong[150:-150, :] = 255  # retain all corner markers, remove the form
    try:
        recognize(wrong)
    except ValueError:
        print('PASS wrong layout despite valid markers: rejected')
    else:
        raise AssertionError('Wrong layout accepted')


if __name__ == '__main__':
    main()
