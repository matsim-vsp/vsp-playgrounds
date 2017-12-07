#!/usr/bin/python

# Created by amit on 21.10.17

import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path


def plotDecisionVariableParameters(parentDir, regexStr, lastItr, statsFileName, numberOfRowsToPrint):
    opdytsDir = list(range(0, 10, 1))
    for i in opdytsDir:
        file = parentDir + '/_' + str(i) + '/' + statsFileName
        if Path(file).is_file() :
            data = pd.read_csv(file, sep="\t")
            result = data.filter(regex=regexStr)  # take columns starting with asc
            x = result.index.values

            plt.figure(figsize=(10, 5))
            outfile = parentDir + '/decisionVariable_for'+str(numberOfRowsToPrint)+'Its'+str(i)+'.pdf'

            for colName in list(result):
                y = result.loc[:, colName]
                if numberOfRowsToPrint> len(x):
                    plt.plot(x, y, '-.', markersize=0.8)
                else:
                    plt.plot(x[:numberOfRowsToPrint], y[:numberOfRowsToPrint], '-o', markersize=2)  # plt.plot(x[:50], y[:50], '-o', markersize=2)

            plt.xlabel('iteration')
            plt.ylabel('decision variable parameter')
            plt.legend(fontsize=12, loc='upper center', ncol=5, frameon=False)
            plt.savefig(outfile)
            plt.clf()
            plt.close('all')
        else:
            pass


# filesDir = '/Users/amit/Documents/repos/runs-svn/opdyts/patna/allModes/calibration/output/'
#
# for i in list(range(401, 497, 1)):
#     parentDir = filesDir + 'run' + str(i) + '/'
#     print("plotting from dir " + parentDir)
#     plotModalASC(parentDir, "asc", 40, '/opdyts_modalStats.txt', 50)

filesDir = '/Users/amit/Documents/gitlab/runs-svn/opdytsForSignals/greenWaveSingleStream_shortLinks_intervalDemand/opdyts_StartOffset0_stepSize7random_30it_score/'

plotDecisionVariableParam