#
# 
# Author: aagarwal
###############################################################################

library(ggplot2)

inputFile=read.delim(commandArgs()[3],header=T,sep="\t")
#inputFile=read.delim("personTripInfo.txt",header=T,sep="\t")
outputFile=pdf(commandArgs()[4],width=10,height=6)

usefulData=subset(inputFile, tripTravelTimeInHr!=0)
usefulDataWithoutFreight=subset(usefulData, userGroup!="Freight")

g=ggplot(usefulDataWithoutFreight,aes(userGroup,tripSpeedInKPH))
g+geom_boxplot(aes(colour=tripMode))+theme(legend.position="bottom")

dev.off()
