### conda install diagrams
from diagrams import Cluster, Diagram, Edge
from diagrams.custom import Custom
import os
os.environ['PATH'] += os.pathsep + 'C:/Program Files/Graphviz/bin/'

graphattr = {     #https://www.graphviz.org/doc/info/attrs.html
    'fontsize': '22',
}

nodeattr = {   
    'fontsize': '22',
    'bgcolor': 'lightyellow'
}

eventedgeattr = {
    'color': 'red',
    'style': 'dotted'
}
evattr = {
    'color': 'darkgreen',
    'style': 'dotted'
}
with Diagram('logisticmapArch', show=False, outformat='png', graph_attr=graphattr) as diag:
  with Cluster('env'):
     sys = Custom('','./qakicons/system.png')
### see https://renenyffenegger.ch/notes/tools/Graphviz/attributes/label/HTML-like/index
     with Cluster('ctxlogisticmap', graph_attr=nodeattr):
          mapservice=Custom('mapservice','./qakicons/symActorWithobjSmall.png')
          viewer=Custom('viewer','./qakicons/symActorWithobjSmall.png')
          callerforquicktesting=Custom('callerforquicktesting','./qakicons/symActorWithobjSmall.png')
     callerforquicktesting >> Edge(color='magenta', style='solid', decorate='true', label='<evallogistic<font color="darkgreen"> replylogistic</font> &nbsp; >',  fontcolor='magenta') >> mapservice
     mapservice >> Edge(color='blue', style='solid',  decorate='true', label='<showgraph &nbsp; >',  fontcolor='blue') >> viewer
diag
