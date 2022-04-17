#!/usr/bin/env python
# run script with command: python src/utils/Plotter.py

import sys
import pandas as pd
import numpy as np
from matplotlib import pyplot as plt


def main():
    df = pd.read_csv("../../out/solutions/csv/or_pas_dept4_short01_move_info.csv")
    plot_accepted_moves(df)


def plot_savings_per_move(df):
    patient_savings = [0, 0, 0, 0]
    load_savings = [0, 0, 0, 0]

    for idx, row in df.iterrows():
        if row.get("accepted") == 1:
            patient_savings[row.get("type")] += row.get("patient_savings")
            load_savings[row.get("type")] += row.get("load_savings")

    plt.bar(np.arange(len(patient_savings)), patient_savings, width=0.3, color="blue")
    plt.bar(np.arange(len(load_savings)) + 0.3, load_savings, width=0.3, color="orange")
    plt.show()


def plot_accepted_moves(df):
    count = [0, 0, 0, 0]

    for idx, row in df.iterrows():
        if row.get("accepted") == 1:
            count[row.get("type")] += 1

    plt.bar(np.arange(len(count)), count, color="orange")
    plt.show()


def plot_cost_evolution(df):
    plt.scatter(df.get("load_cost"), df.get("patient_cost"), c="orange")
    plt.xlabel("load cost")
    plt.ylabel("patient cost")
    plt.show()


if __name__ == "__main__":
    main()
