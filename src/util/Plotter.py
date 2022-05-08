#!/usr/bin/env python
# run script with command: python src/utils/Plotter.py

import sys
import pandas as pd
import numpy as np
from matplotlib import pyplot as plt
import plotly.express as px


def main():
    instance = "or_pas_dept4_short01"
    df_moves = pd.read_csv("solutions/csv/${instance}_move_info.csv")
    df_schedule = pd.read_csv("solutions/csv/${instance}_schedule.csv")
    generations, accepts = get_move_generations(df_moves)
    df_moves = remove_unaccepted_moves(df_moves)
    plot_cost_time_evolution(df_moves)
    plot_cost_cost_evolution(df_moves)
    plot_generated_moves(generations, accepts)
    plot_accepts_per_generated_move(accepts, generations)
    plot_accepts_per_specific_generated_move(accepts, generations)
    plot_gantt_chart(df_schedule)


def get_move_generations(df):
    generations = [0, 0, 0, 0]
    accepts = [0, 0, 0, 0]
    for _, row in df.iterrows():
        generations[row.get("type")] += 1
        if row.get("accepted") == 1:
            accepts[row.get("type")] += 1
    return generations, accepts


def remove_unaccepted_moves(df):
    unaccepted_moves = []
    for idx, row in df.iterrows():
        if row.get("accepted") == 0:
            unaccepted_moves.append(idx)
    return df.drop(unaccepted_moves)


def plot_cost_time_evolution(df):
    plt.plot(df.get("id"), df.get("load_cost"), c="orange")
    plt.plot(df.get("id"), df.get("patient_cost"), c="blue")
    plt.xlabel("move number")
    plt.ylabel("cost")
    plt.show()


def plot_cost_cost_evolution(df):
    plt.plot(df.get("load_cost"), df.get("patient_cost"), c="orange")
    plt.xlabel("load cost")
    plt.ylabel("patient cost")
    plt.show()


def plot_generated_moves(generated_moves, accepted_moves):
    plt.bar(np.arange(len(generated_moves)),
            generated_moves, width=0.3, color="orange")
    plt.bar(np.arange(len(accepted_moves)) + 0.3,
            accepted_moves, width=0.3, color="green")
    plt.show()


def plot_accepts_per_generated_move(accepted_moves, generated_moves):
    total = np.sum(generated_moves)
    accepts_per_generate = []
    for i in range(len(accepted_moves)):
        accepts_per_generate.append(accepted_moves[i] / total)

    plt.bar(np.arange(len(generated_moves)),
            accepts_per_generate, color="orange")
    plt.show()


def plot_accepts_per_specific_generated_move(accepted_moves, generated_moves):
    accepts_per_type = []
    for i in range(len(accepted_moves)):
        accepts_per_type.append(accepted_moves[i] / generated_moves[i])

    plt.bar(np.arange(len(generated_moves)), accepts_per_type, color="orange")
    plt.show()


def plot_savings_per_move(df):
    print("todo")


def plot_savings_per_accepted_move(df):
    print("todo")


def plot_gantt_chart(df):
    fig = px.timeline(df, x_start="Admission", x_end="Discharge", y="RoomCode", color="Treatment", hover_name="Name",
                      facet_row_spacing=0, facet_row="Department", category_orders="RoomCode")
    fig.show()



if __name__ == "__main__":
    main()
