module mymodule(
    input clk_2g,
    input clk_3g,
    input rst
);


    //clk domain 3g
    reg [7:0] cnt_3g;
    always @(posedge clk_3g) begin
        if(rst)
            cnt_3g <= 8'd0;
        else if(cnt_3g == 8'd2)
            cnt_3g <= 8'd0;
        else 
            cnt_3g <= cnt_3g + 1'b1;
    end

    wire testa;
    wire testb;
    assign testa = cnt_3g == 8'd2;
    assign testb = ~(cnt_3g == 8'd0);



    //clk domain 2g
    reg cnt_2g;

    always @(posedge clk_2g) begin
        if(rst)
            cnt_2g <= 1'b0;
        else 
            cnt_2g <= ~cnt_2g;
    end

    reg sample0_a, sample1_a;
    reg sample0_b, sample1_b;

    always @(posedge clk_2g) begin
        if(rst) begin
            sample0_a <= 1'b0;
            sample1_a <= 1'b0;
            sample0_b <= 1'b0;
            sample1_b <= 1'b0;
        end
        else begin
            if(~cnt_2g) begin
                sample0_a <= testa;
                sample0_b <= testb;
            end
            else if(cnt_2g) begin
                sample1_a <= testa;
                sample1_b <= testb;
            end
        end
    end

    wire a_valid, b_valid;
    assign a_valid = ~sample0_a && sample1_a;
    assign b_valid = ~sample0_b && sample1_b;

endmodule