#!/usr/bin/env python3
import sys
import pickle

pickleFileName = "/tmp/wordMap"

def readGloveFile(file, pFile):
    wordMap = dict()
    lineCount = 0
    pickleCount = 0
    with open(file, 'r') as file:
        isFirstLine = True
        un = "(unknown)"
        for line in file:
            words = line.split()
            isFirst = True
            vector = list()
            for word in words:
                if isFirst:
                    if isFirstLine:
                        wordMap[un] = list()
                        wordMap[un].append(float(word))
                        isFirstLine = False
                    else:
                        un = word
                        wordMap[word] = list()
                    isFirst = False
                else:
                    wordMap[un].append(float(word))
            lineCount = lineCount + 1
            if (lineCount % 100000) == 0:
                a_file = open(pFile + str(pickleCount) + ".pkl", "wb")
                pickle.dump(wordMap, a_file)
                a_file.close()
                wordMap.clear()
                pickleCount = pickleCount + 1
                print(str(lineCount) + " lines parsed")
            # if lineCount > 10:
            #     break
    return lineCount

# python3 PickleMaker.py /mnt/e/glove.840B.300d.10f.txt/glove.840B.300d.10f.txt

if __name__ == "__main__":
    gloveFileName = sys.argv[1]
    lc = readGloveFile(gloveFileName, pickleFileName)
    print('done reading ' + str(lc) + ' lines')
