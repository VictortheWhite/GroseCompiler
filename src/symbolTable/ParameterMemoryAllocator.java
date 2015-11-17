package symbolTable;

import java.util.ArrayList;
import java.util.List;


public class ParameterMemoryAllocator implements MemoryAllocator {
	MemoryAccessMethod accessor;
	final int startingOffset;
	int currentOffset;
	int minOffset;
	String baseAddress;
	List<Integer> bookmarks;
	List<MemoryLocation> memLocations;
	
	// return variables use positive allocate schema
	static int i = 0;
	
	
	public ParameterMemoryAllocator(MemoryAccessMethod accessor, String baseAddress, int startingOffset) {
		this.accessor = accessor;
		this.baseAddress = baseAddress;
		this.startingOffset = startingOffset;
		this.currentOffset = startingOffset;
		this.minOffset = startingOffset;
		this.bookmarks = new ArrayList<Integer>();
		this.memLocations = new ArrayList<MemoryLocation>();
		i = 0;
	}
	public ParameterMemoryAllocator(MemoryAccessMethod accessor, String baseAddress) {
		this(accessor, baseAddress, 0);
	}

	@Override
	public MemoryLocation allocate(int sizeInBytes) {
		currentOffset -= sizeInBytes;
		updateMin();
		MemoryLocation location = new MemoryLocation(accessor, baseAddress, currentOffset);
		this.memLocations.add(location);
		
		return location;
	}
	

	
	private void updateMin() {
		if(minOffset > currentOffset) {
			minOffset = currentOffset;
		}
	}
	private void normalizeOffset() {
		for(MemoryLocation location : this.memLocations) {
			location.setOffset(location.getOffset() - minOffset);
		}
	}
	

	@Override
	public String getBaseAddress() {
		return baseAddress;
	}

	@Override
	public int getMaxAllocatedSize() {
		return startingOffset - minOffset;
	}
	
	@Override
	public void saveState() {
		bookmarks.add(currentOffset);
	}
	@Override
	public void restoreState() {
		assert bookmarks.size() > 0;
		int bookmarkIndex = bookmarks.size()-1;
		currentOffset = (int) bookmarks.remove(bookmarkIndex);
		
		if(bookmarks.size() == 0)
			normalizeOffset();
	}
}
