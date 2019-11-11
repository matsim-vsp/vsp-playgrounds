# SET OPTIONS:
# set terminal pdf {monochrome|color|colour}
#                  {{no}enhanced}
#                  {fname "<font>"} {fsize <fontsize>}
#                  {font "<fontname>{,<fontsize>}"}
#                  {linewidth <lw>} {rounded|butt}
#                  {solid|dashed} {dl <dashlength>}}
#                  {size <XX>{unit},<YY>{unit}}
# The default size for PDF output is 5 inches by 3 inches

set termoption dash

# Define varibales
LEQ = "{/Symbol \243}"

####################

#set terminal pdf size 8in,6in font "Helvetica,14"
set terminal pdf size 4in,3in font "Helvetica,10"

set output "activityTypes.pdf"

#set title "Activity Types"
#set title "(e)"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

set key below
#set key 100,100
#set key box

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Activity Type"
set ylabel "Relative Frequency"
#set xtics rotate 90
#set xtics 2000,1
#set xtics 0,5

set yrange [0:0.45]
#set yrange [0:0.60]

set style data histogram

# gap with odd value effects that colums are moved so that they appear centered above the xlabels
set style histogram clustered gap 1

#set style fill {empty | {transparent} solid {<density>} | {transparent} pattern {<n>}} {border {lt} {lc <colorspec>} | noborder}

#set style fill solid 0.50 border -1
set style fill solid 0.6 noborder

plot "activityTypes.txt" using 3:xtic(1) title "Simulation" linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/activityTypes.txt" using 3 title "Survey" linecolor rgb "blue"


####################

set output "averageTripSpeedBeeline.pdf"

#set title "Average Trip Speed (Beeline)"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

#set legend next to y-axis with a box
set key below
#set key 100,100
#set key box

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Average Trip Speed [km/h]"
set ylabel "Relative Frequency"
set xrange[0:60]
set yrange[0:0.25]
set xtics (0, 10, 20, 30, 40, 50, 60)

plot "averageTripSpeedBeeline.txt" using 1:3 title "Simulation" with lines linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/averageTripSpeedBeeline.txt" using 1:3 title "Survey" with lines linetype 5 linecolor rgb "blue"

####################

set output "averageTripSpeedBeelineCumulative.pdf"

#set title "Average Trip Speed (Beeline)"
#set title "(d)"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

#set legend next to y-axis with a box
set key below
#set key 100,100
#set key box

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Average Trip Speed [km/h]"
set ylabel "Relative Cummulative Frequency"
set xrange[0:60]
set yrange[0:1.0]
set xtics (LEQ."10" 10, LEQ."20" 20, LEQ."30" 30, LEQ."40" 40, LEQ."50" 50, LEQ."60" 60)

plot "averageTripSpeedBeelineCumulative.txt" using 1:3 title "Simulation" with lines linewidth 2 linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/averageTripSpeedBeelineCumulative.txt" using 1:3 title "Survey" with lines linetype 5 linewidth 2 linecolor rgb "blue"

####################

set output "averageTripSpeedRouted.pdf"

#set title "Average Trip Speed (Routed)"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

#set legend next to y-axis with a box
set key below
#set key 100,100
#set key box

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Average Trip Speed [km/h]"
set ylabel "Relative Frequency"
set xrange[0:60]
set yrange[0:0.25]
set xtics (0, 10, 20, 30, 40, 50, 60)

plot "averageTripSpeedRouted.txt" using 1:3 title "Simulation" with lines linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/averageTripSpeedRouted.txt" using 1:3 title "Survey" with lines linetype 5 linecolor rgb "blue"

####################

set output "tripDistanceBeeline.pdf"

#set title "Trip Distance (Beeline)"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

# set legend next to y-axis with a box
set key below
#set key 100,100
#set key box

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Trip Distance [km]"
set ylabel "Relative Frequency"
set xrange[0:60]
set yrange[0:0.4]
set xtics (0, 10, 20, 30, 40, 50, 60)

plot "tripDistanceBeeline.txt" using 1:3 title "Simulation" with lines linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/tripDistanceBeeline.txt" using 1:3 title "Survey" with lines linetype 5 linecolor rgb "blue"

####################

set output "tripDistanceBeelineCumulative.pdf"

#set title "Trip Distance (Beeline)"
#set title "(b)"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

# set legend next to y-axis with a box
set key below
#set key 100,100
#set key box

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Trip Distance [km]"
set ylabel "Relative Cummulative Frequency"
set xrange[0:60]
set yrange[0:1.0]
set xtics (LEQ."10" 10, LEQ."20" 20, LEQ."30" 30, LEQ."40" 40, LEQ."50" 50, LEQ."60" 60)

plot "tripDistanceBeelineCumulative.txt" using 1:3 title "Simulation" with lines linewidth 2 linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/tripDistanceBeelineCumulative.txt" using 1:3 title "Survey" with lines linetype 5 linewidth 2 linecolor rgb "blue"

#####################

set output "tripDistanceRouted.pdf"

#set title "Trip Distance (Routed)"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

# set legend next to y-axis with a box
set key below
#set key 100,100
#set key box

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Trip Distance [km]"
set ylabel "Relative Frequency"
set xrange[0:60]
set yrange[0:0.4]
set xtics (0, 10, 20, 30, 40, 50, 60)

plot "tripDistanceRouted.txt" using 1:3 title "Simulation" with lines linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/tripDistanceRouted.txt" using 1:3 title "Survey" with lines linetype 5 linecolor rgb "blue"

#####################

set output "tripDuration.pdf"

#set title "Trip Duration"
#set title "(b)"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

# set legend next to y-axis with a box
set key below
#set key 100,100
#set key box

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Trip Duration [min]"
set ylabel "Relative Frequency"
set xrange[0:120]
set yrange[0:0.2]
set xtics (0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120)

plot "tripDuration.txt" using 1:3 title "Simulation" with lines linewidth 2 linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/tripDuration.txt" using 1:3 title "Survey" with lines linetype 5 linewidth 2 linecolor rgb "blue"

#####################

set output "tripDurationCumulative.pdf"
#set title "(c)"

#set title "Trip Duration"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

# set legend next to y-axis with a box
set key below
#set key 100,100
#set key box

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Trip Duration [min]"
set ylabel "Relative Cummulative Frequency"
set xrange[0:60]
set yrange[0:1.0]
set xtics (LEQ."10" 10, LEQ."20" 20, LEQ."30" 30, LEQ."40" 40, LEQ."50" 50, LEQ."60" 60, LEQ."70" 70, LEQ."80" 80, LEQ."90" 90, LEQ."100" 100, LEQ."110" 110, LEQ."120" 120)


plot "tripDurationCumulative.txt" using 1:3 title "Simulation" with lines linewidth 2 linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/tripDurationCumulative.txt" using 1:3 title "Survey" with lines linetype 5 linewidth 2 linecolor rgb "blue"

#####################

set output "departureTime.pdf"

#set title "Departure Times"
#set title "(a)"

set datafile commentschars "#%"
set grid
set datafile missing "-"
set style data linespoints

# set legend next to y-axis with a box
set key below

#set format y "%10.0f"
set format y "%1.2f"

set xlabel "Departure Time [h]"
set ylabel "Relative Frequency"
set xrange[0:24]

set yrange[0:0.12]
#set yrange[0:0.30]

set xtics (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)

plot "departureTime.txt" using 1:3 title "Simulation" with lines linewidth 2 linecolor rgb "red",\
"../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/".ARG1."/departureTime.txt" using 1:3 title "Survey" with lines linetype 5 linewidth 2 linecolor rgb "blue"