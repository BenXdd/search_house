package com.benx.service.user;


import java.util.List;

/**
 * 通用多结果Service返回结构
 */
public class ServiceMultiResut<T> {

    private long total; //我们的列表/数据库总共有多少 条 数据
    private List<T> result;  //service 接口返回的列表数据

    /**
     * 返回当前结果集    空-->0   非空-->list.size();
     * @return
     */
    public int getResultSize(){
        if (this.result == null){
            return 0;
        }
        return this.result.size();
    }

    public ServiceMultiResut(long total, List<T> result) {
        this.total = total;
        this.result = result;
    }
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getResult() {
        return result;
    }

    public void setResult(List<T> result) {
        this.result = result;
    }
}
