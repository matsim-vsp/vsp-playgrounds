#!/usr/bin/python

# Created by amit on 21.10.17

from numpy import genfromtxt
import matplotlib.pyplot as plt


def getXYFromInputFile ( inputFile ):
    # numpy.loadtxt can be used when no data is missing
    f=genfromtxt(inputFile, delimiter="\t", skip_header=1, usecols=(0,1) )
    y1=f[:,0] # first column
    y2=f[:,1] # second column
    x=list(range(0,len(y1),1)) # iterations
    return x,y1,y2


saveFig = True

dir = '/Users/amit/Documents/repos/runs-svn/opdyts/patna/allModes/calibration/output/'
runDirs = [434,435,436]
# runDirs = [46,47,48]
# runDirs = [10,11,12]

cols=['gray','lightgray']
shapes=['s','^']

outFileSuffix='_'
for suffix in runDirs:
    outFileSuffix=outFileSuffix+'run'+str(suffix)+'_'


outFile = '/Users/amit/Documents/git/opdytsPaper/fullPaper/figs/patnaAllModes/convergence'+outFileSuffix+'subPlot.pdf'

# TODO probably, better to read file only once rather than reading it twice
# anyFile=dir + '/' + runDirs[0] + '/axial_fixedVariation/opdyts.con'
# labels=open(anyFile).readlines().pop(0).split("\t")

labels=['raw obj. fun. value', 'avg. obj. fun. value']

f, arrPlot = plt.subplots(nrows=len(runDirs), sharex=True, sharey=True, figsize=(10, 4))

i = 0  # an index to get the labels
for runNr in runDirs:
    inputFile = dir + '/run' + str(runNr) + '/opdyts.con'
    arr=getXYFromInputFile(inputFile)
    x= arr[0]
    y1=arr[1]
    y2=arr[2]
    arrPlot[i].plot(x, y1, shapes[0], markersize=3, color=cols[0], label=labels[0])
    arrPlot[i].plot(x, y2, shapes[1], markersize=3, color=cols[1], label=labels[1])
    arrPlot[i].set_title('run'+ str(runDirs[i]),fontsize=10)
    arrPlot[i].legend(fontsize=11, loc='upper left', ncol=2, frameon=False, markerscale=1, bbox_to_anchor = (0.55,1.34) if len(runDirs)>2 else (0.55,1.24))
    plt.setp(arrPlot[i].spines.values(), color='lightgray') # change border color
    i = i+1

# Fine-tune figure; make subplots close to each other and hide x ticks for
# all but bottom plot.
# f.subplots_adjust(hspace=0)
# plt.setp([a.get_xticklabels() for a in f.axes[:-1]], visible=False)

plt.ylim(0, 1)
plt.tick_params(axis = 'both', labelsize = 10)

f.text(0.5, 0.0, 'iteration', ha='center', fontsize=12)
f.text(0.0, 0.5, 'value of objective function', va='center', rotation='vertical', fontsize=12)

plt.tight_layout()

if saveFig:
    plt.savefig(outFile)
else:
    plt.show()