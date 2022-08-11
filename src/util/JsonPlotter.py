#!/usr/bin/env python
# run script with command: python src/utils/Plotter.py

import sys
import json
import pandas as pd
import numpy as np
from matplotlib import pyplot as plt
import plotly.express as px

def main():
    file = open(sys.argv[1]) 
    data = json.load(file)
    for iteration in data['iterations'][0:50]:
        patient_costs = []
        equity_costs = []
        for sol in iteration['solution_archive']:
            patient_costs.append(int(sol['patient_cost']))
            equity_costs.append(float(sol['equity_cost']))
        fig, ax = plt.subplots()
        ax.scatter(patient_costs, equity_costs, c='black')

        if 'harvested_solution' in iteration:
            sol = iteration['harvested_solution']
            ax.scatter(int(sol['patient_cost']),
                    float(sol['equity_cost']),c='red')

        if 'natural_solution' in iteration:
            sol = iteration['natural_solution']
            ax.scatter(int(sol['patient_cost']),
                    float(sol['equity_cost']),c='purple')


        sol = iteration['final_solution']
        ax.scatter(int(sol['patient_cost']), 
                float(sol['equity_cost']), c='green')

        for rect in iteration['rectangle_archive']:
            x = []
            x.append(int(rect['x_1']))
            x.append(int(rect['x_2']))
            ax.fill_between(x, int(rect['y_2']), int(rect['y_1']), alpha=.4,
                    linewidth=.8)
        plt.show()
    file.close()


def internet():
    np.random.seed(1)
    x = np.linspace(0, 8, 16)
    y1 = 3 + 4*x/8 + np.random.uniform(0.0, 0.5, len(x))
    y2 = 1 + 2*x/8 + np.random.uniform(0.0, 0.5, len(x))

    # plot
    fig, ax = plt.subplots()

    ax.fill_between(x, y1, y2, alpha=.5, linewidth=0)
    ax.plot(x, (y1 + y2)/2, linewidth=2)

    ax.set(xlim=(0, 8), xticks=np.arange(1, 8),
           ylim=(0, 8), yticks=np.arange(1, 8))

    plt.show()


if __name__ == "__main__":
    main()

