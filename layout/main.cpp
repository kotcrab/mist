#include <ogdf/layered/SugiyamaLayout.h>
#include <ogdf/layered/OptimalRanking.h>
#include <ogdf/layered/MedianHeuristic.h>
#include <ogdf/module/LayerByLayerSweep.h>
#include <ogdf/layered/OptimalHierarchyLayout.h>
#include <ogdf/fileformats/GraphIO.h>

using namespace ogdf;

int main() {
	Graph g;
	GraphAttributes ga(g,
		GraphAttributes::nodeGraphics |
		GraphAttributes::edgeGraphics |
		GraphAttributes::nodeLabel |
		GraphAttributes::edgeStyle |
		GraphAttributes::nodeStyle |
		GraphAttributes::nodeTemplate);
	if (!GraphIO::readGML(ga, g, std::cin)) {
		return 1;
	}
	SugiyamaLayout sl;
	sl.setRanking(new OptimalRanking());
	sl.setCrossMin(new MedianHeuristic());
	OptimalHierarchyLayout* ohl = new OptimalHierarchyLayout();
	ohl->layerDistance(30.0);
	ohl->nodeDistance(25.0);
	ohl->weightBalancing(0.8);
	sl.setLayout(ohl);
	sl.call(ga);
	GraphIO::writeGML(ga, std::cout);
	return 0;
}
