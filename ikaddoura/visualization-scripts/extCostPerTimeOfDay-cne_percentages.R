# "\t" for tab
# ";" for semicolon

############

# inputData1 <- read.csv("policyCaseRunId.congestionTollsPerDepartureTime_car_3600.csv", sep=";")
# x_1 <- inputData1[,1]/3600

# inputData2 <- read.csv("policyCaseRunId.noiseTollsPerDepartureTime_car_3600.csv", sep=";")
# x_2 <- inputData2[,1]/3600

# inputData3 <- read.csv("policyCaseRunId.airPollutionTollsPerDepartureTime_car_3600.csv", sep=";")
# x_3 <- inputData3[,1]/3600

############

# inputData1 <- read.csv("policyCaseRunId.congestionTollsPerDepartureTime_car_3600.csv", sep=";")
# x_1 <- inputData1[,1]/3600

# inputData2 <- read.csv("policyCaseRunId.noiseTollsPerDepartureTime_car_3600.csv", sep=";")
# x_2 <- inputData2[,1]/3600

# inputData3 <- read.csv("policyCaseRunId.airPollutionTollsPerDepartureTime_car_3600.csv", sep=";")
# x_3 <- inputData3[,1]/3600

############

# inputData1 <- read.csv("policyCaseRunId.congestionTollsPerDepartureTime_car_3600.csv", sep=";")
# x_1 <- inputData1[,1]/3600

# inputData2 <- read.csv("policyCaseRunId.noiseTollsPerDepartureTime_car_3600.csv", sep=";")
# x_2 <- inputData2[,1]/3600

# inputData3 <- read.csv("policyCaseRunId.airPollutionTollsPerDepartureTime_car_3600.csv", sep=";")
# x_3 <- inputData3[,1]/3600

############

inputData1 <- read.csv("policyCaseRunId.congestionTollsPerDepartureTime_car_3600.csv", sep=";")
x_1 <- inputData1[,1]/3600

inputData2 <- read.csv("policyCaseRunId.noiseTollsPerDepartureTime_car_3600.csv", sep=";")
x_2 <- inputData2[,1]/3600

inputData3 <- read.csv("policyCaseRunId.airPollutionTollsPerDepartureTime_car_3600.csv", sep=";")
x_3 <- inputData3[,1]/3600

############

# first graph
y_1 <- inputData1[,2]
col_1 = "blue"
type_1 = "o"
pointType_1 = 21
lineType_1 = 1

# further graphs
y_2 <- inputData2[,2]
col_2 = "red"
type_2 = "o"
pointType_2 = 22
lineType_2 = 1

y_3 <- inputData3[,2]
col_3 = "darkgreen"
type_3 = "o"
pointType_3 = 23
lineType_3 = 1

y_4 <- y_1 + y_2 + y_3
col_4 = "purple"
type_4 = "o"
pointType_4 = 24
lineType_4 = 1

# percentages
y_1 <- y_1/y_4
y_2 <- y_2/y_4
y_3 <- y_3/y_4

minY <- 0 # min(c(y_1, y_2, y_3, y_4)) # adjust for further graphs
maxY <- 1 # max(c(y_1, y_2, y_3)) + 0.1 # adjust for further graphs
minX <- 5
maxX <- 23

# pdf("/Users/ihab/Documents/workspace/shared-svn/papers/2017/cne/paper/graphics/berlin-extCostPerTimeOfDay-cne-r-percentage.pdf", width=8, height=6)

# plots the diagram with first graph
plot(x_1, y_1, type=type_1, lty=lineType_1, pch=pointType_1, col=col_1, xlim=c(minX,maxX), ylim=c(minY, maxY+0.01), xlab="Time of day [h]", ylab="Contribution to average toll level per trip [%]")
par (xaxs="i", yaxs="i", lab=c(maxX,20,0), font.axis=1, cex.axis=1, bg="white")

# for further graph
lines(x_2, y_2, type=type_2, col=col_2, lty=lineType_2, pch=pointType_2)
lines(x_3, y_3, type=type_3, col=col_3, lty=lineType_3, pch=pointType_3)
#lines(x_1, y_4, type=type_4, col=col_4, lty=lineType_4, pch=pointType_4)


# ...

legend(minX + 1, maxY-0.01, c("Congestion", "Noise", "Air Pollution"), col=c(col_1, col_2, col_3), lty=c(lineType_1, lineType_2, lineType_3), pch=c(pointType_1, pointType_2, pointType_3), ncol = 3) # adjust for further graphs

# title(main="title", font.main=2)
# dev.off()