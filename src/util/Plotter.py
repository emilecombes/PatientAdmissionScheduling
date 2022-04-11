#!/usr/bin/env python
# run script with command: python src/utils/plotSolutionsFromCSV.py

import sys
import pandas as pd
from matplotlib import pyplot as plt


plt.rcParams["figure.figsize"] = [10.00, 5.00]
plt.rcParams["figure.autolayout"] = True
columns = ["Time", "Total"]
df = pd.read_csv("out/Solutions-" + sys.argv[1], usecols=columns)
print("Contents in csv file:\n", df)
plt.plot(df.Time, df.Total)
plt.show()