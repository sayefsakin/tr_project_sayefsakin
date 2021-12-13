import pickle
from urllib.parse import unquote

from fastapi import APIRouter

router = APIRouter()
pickleFileName = "/tmp/wordMap"
pickleCount = 0
output = list()
while True:
    try:
        pFile = open(pickleFileName + str(pickleCount) + ".pkl", "rb")
        pickleCount = pickleCount + 1
    except FileNotFoundError:
        break
    output.append(pickle.load(pFile))

@router.get('/{qWord}')
def get_procMetrics(qWord: str):
    uqword = unquote(qWord)
    for ol in output:
        if uqword in ol:
            return ' '.join(map(str, ol[uqword]))
    return ' '.join(map(str, output[0]["(unknown)"]))
