"ui";

const page = compose.state<number>(0);
const label = compose.state<string | null>(null);

const total: number = 3;

let checked = compose.state(false);

compose.mount(() => {
    return (
        <compose.Column padding={16} spacing={-2} weight={1e3} w={0x10} enabled={true}>
            <compose.Text key='first' text={'double & quoted'} style='titleMedium' />
            <compose.Text text={'back\\slash'} color={[1, 2]} shape={{shape: 'rounded', radius: 12}} />
            <compose.Text text={'Page ' + page.value} maxLines={page.value} />
            <compose.TopAppBar title='Plain title' />
            <compose.TopAppBar title={<compose.Text text='Element title' />} />
            <compose.TopAppBar title={<compose.Text text='Fragment title' />} />
            <compose.OutlinedTextField text='Empty slot' label={<></>} />
            <compose.Scaffold topBar={<compose.TopAppBar title='Slots object' />} />
            <compose.Column>
                <compose.Text text='a' />
                b
                {label.value}
            </compose.Column>
            <compose.Switch checked={checked.value} onCheckedChange={(value: boolean) => { checked.value = value; }} />
            <compose.Box modifier={compose.modifier().fillMaxWidth().clickable(() => page.value++).padding(8)} />
            <compose.Box modifier={base.then(compose.modifier().padding(4))} />
        </compose.Column>
    );
}, {theme: {dark: false}});
