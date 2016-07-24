import org.apache.spark.sql._
import org.apache.spark._
import sys.process._
import scala.language.postfixOps

object CreateStructUniverse {

    val path = "/ufrc/roitberg/qasdfgtyuiop/05_create_struct_universe"
    val gdb = "/ufrc/roitberg/qasdfgtyuiop/gdb13"
    
    def parse(str:String):StructureUniverse = {
        val l = str.split(raw"\s+",2)
        StructureUniverse(smiles=l(0),mass=l(1).toFloat)
    }
    
    def main(args: Array[String]): Unit = {
        val session = SparkSession.builder.appName("05_create_struct_universe").getOrCreate()
        import session.implicits._
        
        // get list of smiles
        val mid_structure = session.read.parquet("outputs/03/mid_structure").as[MIDStruct]
        val smiles_nist = mid_structure.map(_.structure).distinct()
        val filenames = Range(1,14).map(j=>path+s"/$j.smi")
        val smiles_gdb = filenames.map(session.read.text(_).as[String]).reduce(_.union(_))
        val smiles = smiles_nist.union(smiles_gdb).distinct()
        print("number of structures: "); println(smiles.count())
        
        // apply transformations to smiles to generate parquet
        val smstr = smiles.rdd.pipe(path+"/tools/calc-mass.py").toDS()
        val universe = smstr.map(parse)
        
        universe.show()
        universe.write.parquet("outputs/05/universe")
    }

}
