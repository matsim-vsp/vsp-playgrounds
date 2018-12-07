#!/usr/bin/python

# Created by amit on 21.10.17

from numpy import genfromtxt
from matplotlib import pyplot

#a function which returns x, y1, y2
def getXYFromInputFile ( inputFile ):
    # numpy.loadtxt can be used when no data is missing
    f=genfromtxt(inputFile, delimiter="\t", skip_header=1, usecols=(0,1) ) # reading first two coloumns of input file, skipping first row (header)
    y1=f[:,0] # first column
    y2=f[:,1] # second column
    x=list(range(0,len(y1),1)) # iterations
    return x,y1,y2

saveFig = True

pyplot.figure(figsize=(10, 5)) # size of the canvas

dir = '/Users/amit/Documents/repos/runs-svn/opdyts/patna/networkModes/calibration/output/'
runDirs = [201,202,203]
# runDirs = [46,47,48]
# runDirs = [10,11,12]

cols=['dimgray','lightgray']
shapes=['*','.','s','+','^','3']

outFile = '/Users/amit/Documents/git/opdytsPaper/fullPaper/figs/patnaNetworkModes/convergence_run'+str(runDirs[0])+'-'+str(runDirs[2])+'.pdf'

# # TODO probably, better to read file only once rather than reading it twice
# anyFile=dir + '/' + runDirs[0] + '/axial_fixedVariation/opdyts.con'
# labels=open(anyFile).readlines().pop(0).split("\t")

labels=['raw obj. fun. value', 'avg. obj. fun. value']

i=0 #an index to get the labels
for runNr in runDirs:
    inputFile = dir + '/run' + str(runNr) + '/opdyts.con'
    arr=getXYFromInputFile(inputFile)
    x= arr[0]
    y1=arr[1]
    y2=arr[2]
    pyplot.plot(x, y1, shapes.pop(0), markersize=0.4, color=cols[0], label=labels[0]+' (run'+ str(runDirs[i]) +') ' )
    pyplot.plot(x, y2, shapes.pop(0), markersize=0.4, color=cols[1], label=labels[1]+' (run'+ str(runDirs[i]) +') ' )
    i = i+1

pyplot.ylim(0, 1)
pyplot.xlabel('iteration', fontsize=18)
pyplot.ylabel('value of objective function', fontsize=16)
pyplot.tick_params(axis = 'both', labelsize = 14)

pyplot.legend(fontsize=12,loc='upper center', ncol=3, bbox_to_anchor=(0.5,1.175),frameon=False, markerscale=16.)

if(saveFig):
    pyplot.savefig(outFile)
else:
    pyplot.show()


# ax.legend(loc='upper center', bbox_to_anchor=(0.5, 1.05),
#           ncol=3, fancybox=True, shadow=True)