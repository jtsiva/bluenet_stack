package nd.edu.bluenet_stack;

public class LayerBase{

	protected Reader mReadCB = null;
	protected Writer mWriteCB = null;
	protected Query mQueryCB = null;
	protected String mID = null;

	public void setReadCB(Reader reader) {
		this.mReadCB = reader;
	}
	public void setWriteCB (Writer writer) {
		this.mWriteCB = writer;
	}
	public void setQueryCB (Query query){
		this.mQueryCB = query;
		this.mID = mQueryCB.ask("global.id");
	}
}