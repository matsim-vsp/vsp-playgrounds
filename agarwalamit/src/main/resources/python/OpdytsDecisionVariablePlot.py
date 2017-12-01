#!/usr/bin/python

# Created by amit on 21.10.17

import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path

def plotModalASC(parentDir):
    opdytsDir = list(range(0, 10, 1))
    for i in opdytsDir:
        file = parentDir + '/_' + str(i) + '/opdyts_modalStats.txt'
        if Path(file).is_file() :
            data = pd.read_csv(file, sep="\t")
            result = data.filter(regex="asc")  #take columns starting with asc
            x = result.index.values

            plt.figure(figsize=(10, 5))
            outfile = parentDir + '/modalASCs_'+str(i)+'.pdf'

            for colName in list(result):
                y = result.loc[:, colName]
                plt.plot(x, y, '.', markersize=0.8)

            plt.xlabel('iteration')
            plt.ylabel('ASC')
            plt.legend(fontsize=12, loc='upper center', ncol=5, frameon=False)
            plt.savefig(outfile)
            plt.clf()
            plt.close('all')
        else:
            pass


dir = '/Users/amit/Documents/repos/runs-svn/opdyts/patna/allModes/calibration/output/'

for i in list(range(401,497,1)):
    parentDir = dir + 'run' + str(i) + '/'
    print("plotting from dir " + parentDir)
    plotModalASC(parentDir)

