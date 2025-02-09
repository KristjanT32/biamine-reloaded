package krisapps.biaminereloaded.scoreboard;

public class PaginationInfo {
    private int currentPage;
    private int totalPages;
    private int itemsPerPage;

    public PaginationInfo(int currentPage, int totalPages, int itemsPerPage) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.itemsPerPage = itemsPerPage;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        if (currentPage < 1 || currentPage > totalPages) {return;}
        this.currentPage = currentPage;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public void nextPage() {
        setCurrentPage(currentPage + 1);
    }
}
