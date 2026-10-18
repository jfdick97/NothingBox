"""Real-photo and synthetic regressions; see README for installation."""
from pathlib import Path
import json
import cv2
import numpy as np
from onepager_recognition import analyze, ASSETS, PROFILE

HERE = Path(__file__).resolve().parent

def check(result, expected, tolerance=.22):
    assert result['factors'] == expected['factors'], (result['factors'], expected['factors'])
    for m in range(4):
        for d in range(7):
            actual, target = result['values'][m][d], expected['values'][m][d]
            if target is None:
                assert actual is None, (m,d,actual)
            else:
                assert actual is not None and abs(actual-target) <= tolerance, (m,d,actual,target)


def main():
    photo = cv2.imread(str(HERE/'testdata/onepager-photo.jpg'))
    expected = json.loads((HERE/'testdata/onepager-expected.json').read_text())
    variants = [('real photo', photo), ('90 degrees', cv2.rotate(photo,cv2.ROTATE_90_CLOCKWISE)),
                ('darker', np.clip(photo.astype(float)*.8+15,0,255).astype(np.uint8)),
                ('mild blur',cv2.GaussianBlur(photo,(3,3),.6))]
    for label,image in variants:
        result,overlay=analyze(image);check(result,expected)
        if label=='real photo':cv2.imwrite(str(HERE.parent/'ERKENNUNG_BEISPIEL.jpg'),overlay)
        print('PASS',label,': 28 points, 70 factor positions')
    blank=cv2.imread(str(ASSETS/'onepager_template.png'))
    result,_=analyze(blank)
    assert result['values']==[[None]*7 for _ in range(4)] and result['factors']==[0]*7
    print('PASS blank form: missing remains missing')
    sample=cv2.imread(str(ASSETS/'onepager_sample.jpg'))
    result,_=analyze(sample)
    check(result,json.loads((ASSETS/'onepager_sample_expected.json').read_text()),.16)
    print('PASS synthetic continuous values and both extra factor rows')
    ambiguous=blank.copy()
    x0,y0,x1,y1=PROFILE['sliders'][0][0]
    for value in [3,8]:cv2.circle(ambiguous,(round((x0+(x1-x0)*value/10)*3),round((y0+y1)/2*3)),6,(0,0,0),-1)
    result,_=analyze(ambiguous);assert result['values'][0][0] is None
    print('PASS double mark: withheld')
    for label,image in [('missing corner',blank.copy()),('missing orientation dot',blank.copy())]:
        if label=='missing corner':
            x0,y0,x1,y1=PROFILE['markerRects'][1]
            image[round(y0*3)-6:round(y1*3)+6,round(x0*3)-6:round(x1*3)+6]=255
        else:
            cv2.circle(image,(round(20.8356*3),round(20.726*3)),7,(255,255,255),-1)
        try:analyze(image)
        except ValueError:print('PASS',label,': rejected')
        else:raise AssertionError(label+' was accepted')

if __name__=='__main__':main()
