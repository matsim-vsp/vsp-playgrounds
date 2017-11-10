#!/usr/bin/python

# Created by amit on 03.11.17

import numpy as np
import matplotlib.pyplot as plt

saveFig = True
file = '/Users/amit/Documents/repos/runs-svn/opdyts/patna/networkModes/calibration/output/modalShare.txt'
outFile = '/Users/amit/Documents/git/opdytsPaper/fullPaper/figs/patnaNetworkModes/modalShareRuns.pdf'

with open(file, 'r') as f:
    data = f.readlines()

myData = []
for line in data:
    words = line.split()
    myData.append(words)

headers=myData.pop(0)[1:4]

# use only desired data
runCases=['desiredModalShare','relaxedPlansModalShare','run208','run217','run235']

# filter legends and actual data
numericData = []
for l in myData:
    if l.pop(0) in runCases:
        numericData.append([float(f) for f in l]) # converting to float too
    else:
        pass

# modify legends
runCases[0] = 'desired modal share'
runCases[1] = 'start (relaxed) modal share'

# plot
fig, ax = plt.subplots(figsize=(10,6))

x=np.arange(len(headers))
barWidth= fig.get_size_inches()[0] / len(headers) / len(runCases) / 4 #barWidth depends on width of fig

barList = []
patterns = [ "//" , ".." , "xx", "o", "*" ]
edgeColors = ['black']*len(runCases*len(headers))
for b in numericData:
    bar=ax.bar(x, b, barWidth, edgecolor = edgeColors.pop(0), color = 'lightgray', hatch=2*patterns.pop(0),alpha=1)
    barList.append(bar[0])
    x=x+barWidth

ax.set_ylabel('number of legs', fontsize=12)
ax.set_xlabel('travel modes', fontsize=12)
ax.set_xticks(x - barWidth * len(numericData)/2)
ax.set_xticklabels(headers, fontsize=12)
ax.legend(barList, runCases, ncol=3, frameon=False, bbox_to_anchor = (0.1, 0.99), fontsize=10, handlelength=3, handleheight=1.6)

if saveFig:
    plt.savefig(outFile)
else:
    plt.show()
