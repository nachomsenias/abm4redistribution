from matrix.matrix_utils import MatrixUtils
from plot.heatmap_plot import HeatmapPlot
from pathlib import Path
from utils import get_token
import sys
import json
import logging
import os

# 1. Configuración del logger por consola
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Código antiguo para filtrar valores
# ## Ahora queremos saber como se distribuyen estos puntos.
# df = pd.DataFrame(columns=['Theta', 'Tau', 'Result'])
# for i in range(len(matrix)):
#     for j in range(len(matrix[i])):
#         df_i_j = pd.DataFrame({'Theta': [i], 'Tau': [j], 'Result': [float("%.4f" % matrix[i, j])]})
#         df = pd.concat([df, df_i_j], axis=0)
#
# # Filtrar las filas que tienen un 'Result' mayor de 0.8
# filtered = df[df['Result'] < 0.51]
# filtered = filtered[filtered['Result'] > 0.0]
# sorted_df = filtered.sort_values(by=['Result'], ascending=False)
# sorted_df.to_csv(base_url + data_name + '\\matrix_' + str(a_value) + '_filtered_sorted.csv', sep=';', index=False)
#
# sns.set_theme()
# sns.set(rc={'figure.figsize': (11.7, 8.27)})
# sns.histplot(data=sorted_df, x="Result")
# plt.savefig(base_url + data_name + '\\hisplot.png')

# No usamos main porque este script no se importa en otro módulo
data_name = sys.argv[1]
base_url = sys.argv[2]
# Flag para diferenciar los mapas theta_tau de los mapas theta_a
theta_a = sys.argv[3]
# Como Python es imbécil, hay que tradudir el tipo para tener un booleano.
theta_a = json.loads(theta_a.lower())

logging.info(f"Generating Heatmap plot for {data_name} in {base_url} with flag Theta_a={theta_a}.")
matrix = MatrixUtils.get_matrix(data_name, base_url, theta_a)

suffix = "-grid"
token = get_token(base_url, suffix=suffix)

if len(sys.argv) > 4:
    plot_folder = sys.argv[4]
else:
    plot_folder = f"{base_url}{data_name}"

output_file = f"{plot_folder}\\heatmap_{token}_{data_name}.png"

HeatmapPlot.print_heat_map(data_name, matrix, base_url, theta_a=theta_a, output_file=output_file)
