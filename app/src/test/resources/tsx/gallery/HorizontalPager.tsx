"ui";

let page = compose.state(0);

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={8}>
            <compose.Text key='status' text={'Page ' + (page.value + 1) + ' of 3'} />
            <compose.HorizontalPager key='pager' h={160} page={page.value} onPageChange={function (index) { page.value = index; }}>
                <compose.Card key='one' padding={24}>First page</compose.Card>
                <compose.Card key='two' padding={24}>Second page</compose.Card>
                <compose.Card key='three' padding={24}>Third page</compose.Card>
            </compose.HorizontalPager>
        </compose.Column>
    );
});
